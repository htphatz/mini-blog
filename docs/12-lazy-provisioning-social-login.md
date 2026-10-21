# Phase 12: Social Login & Lazy Provisioning (GitHub via Keycloak Identity Brokering)

## 1. Architectural Motivation (Why Lazy Provisioning?)

Keycloak is the single source of truth for identity (passwords, login, social login). `identity-service` registration (`AuthService.register()`) is an **orchestration Saga** that also creates the business data: a local `users` row, a `Profile` in `profile-service`, and a welcome email through the Outbox (Phase 7).

### The Problem: Users Created Outside `register()` Exist Only in Keycloak
A user created by the first GitHub login, the Keycloak Register page, or the Admin Console never goes through that Saga:

| Place | Has the user? |
| :--- | :--- |
| Keycloak (`USER_ENTITY`, `FEDERATED_IDENTITY`) | Yes |
| `profile-service` (Neo4j `Profile`) | **No** |
| `identity-service` (MySQL `users`, Saga/Outbox) | No (only `register()` writes it) |

`post-service` resolves `displayName` through the gRPC call `getProfileByUserId`, so a user without a profile gets a `NOT_FOUND` when creating a post.

### The Solution: Get-or-Create the Profile From the JWT
The service that owns the data creates it on demand. `profile-service` reads the identity from the verified JWT and creates the profile the first time the user asks for it ("lazy" provisioning).

---

## 2. Under-the-Hood Mechanics

### A. Identity Brokering (GitHub through Keycloak)

```
Browser → Gateway (OIDC Authorization Code + PKCE) → Keycloak login page
            └─ "GitHub" button → GitHub authenticates the user
                 → redirect to the Keycloak broker endpoint
            → Keycloak creates or links the user, then issues its own JWT to the gateway
```

The gateway, the services, and the JWT validation do not change: GitHub is only a login method inside Keycloak.

- **Redirect URI to register in the GitHub OAuth App:** `http://localhost:8484/realms/mini-blog-realm/broker/github/endpoint`.
- **The Client Secret lives only in Keycloak**, never in `.env` or the repository.

### B. Where the Social User Is Stored
Keycloak creates a local user (table `USER_ENTITY`) and a link to GitHub (table `FEDERATED_IDENTITY`: `identity_provider = github`, plus the GitHub id and username). The JWT `sub` is the **Keycloak user id**, not the GitHub id, so every service keeps keying data by `sub` exactly as before (`Profile.userId`).

### C. Account Linking (First Broker Login)
If the email returned by GitHub matches an existing Keycloak user, Keycloak does not create a second user: the **first broker login** flow links the GitHub identity to the existing user, and the `sub` stays the same. By default Keycloak requires unique emails in a realm, so two users with the same email cannot coexist. The *Sync mode* option of the identity provider (`import` / `legacy` / `force`) decides whether the user's data is overwritten on each login (`force`) or only imported the first time.

### D. Lazy vs Eager Provisioning
- **Lazy (this phase):** the profile is created on the first call of `GET /api/v1/profile/me`. Simple, no extra infrastructure, covers every way a user can be created.
- **Eager (Phase 13):** a Keycloak event creates the profile at registration time. See `13-keycloak-kafka-register-events.md`.

---

## 3. Implementation Blueprint in `mini-blog`

### A. Keycloak Configuration
1. Create a GitHub OAuth App with the callback URL above.
2. Admin Console, realm `mini-blog-realm` → Identity providers → **GitHub**: paste the Client ID and Client Secret.
3. Give new users their permissions: Realm settings → User registration → Default roles (`USER`) and/or Default groups (`users`), see `11-keycloak-roles-permissions.md` §3.B. Keycloak applies this to users created by social login too.

### B. `profile-service`: `GET /api/v1/profile/me`
`ProfileController.getMyProfile` → `ProfileService.getOrCreateMyProfile(Jwt)`:

1. Read `sub` from the JWT (the Keycloak user id, the same key as `Profile.userId`).
2. If a profile exists, return it.
3. Otherwise create it from the claims: `firstName` = `given_name` (falling back to `name`, then `preferred_username`), `lastName` = `family_name`. `phoneNumber` and `address` stay empty.

```
Browser → Gateway (session + TokenRelay) → GET /api/v1/profile/me
                                            └─ profile-service: findByUserId(sub) → create if missing
```

Details that matter:
- The call is **idempotent**: `ProfileService.createProfile` also checks for an existing `userId`, so a retry from the `register()` Saga cannot create a second profile.
- The name fallback uses `Stream.of(...)` (not `List.of(...)`): claims missing from the token are `null`, and `List.of` would throw a `NullPointerException`. GitHub tokens typically have no `given_name`.
- `phoneNumber`, `address` (and `lastName`) are no longer `@NotBlank` in `profile-service`'s `ProfileRequest`, because social users do not have them.
- Clients should call this endpoint right after login, before creating posts or comments.

### C. Why It Lives in `profile-service`
`profile-service` owns the profile data, so it creates its own data from the JWT. No call to `identity-service` and no extra `users` copy (id, email, status) is needed. `identity-service` stays unchanged as the Saga + Outbox example for the `register()` flow.

---

## 4. Trade-Off Analysis: Where to Provision Social Users

| Approach | Strength | Weakness |
| :--- | :--- | :--- |
| **Lazy get-or-create in `profile-service` (chosen)** | Smallest change, the owner of the data creates it, works for any user source | Client must call `profile/me`; no welcome email |
| Sync endpoint in `identity-service` (re-run the Saga from JWT claims) | Reuses Saga + Outbox, sends the email | Duplicates the Saga steps, needs a local `users` copy |
| Event-driven from Keycloak (Phase 13) | Immediate, no client dependency, enables the email | Needs a Keycloak plugin and a Kafka consumer |
| Custom Keycloak SPI calling the service over HTTP | Full control | Java extension deployed into Keycloak, the service must be reachable from the container |

---

## 5. Known Limitations

- The client must call `profile/me` before creating a post or comment. Until it does, the profile does not exist.
- Two simultaneous first requests may create two profiles (Neo4j has no unique constraint on `userId`).
- No welcome email is sent for users created outside `register()`. Phase 13 adds it.
- A post's `displayName` is `firstName + " " + lastName`, so a user without a last name gets a trailing space.
- An existing profile is not updated from later JWT claims (for example, the real GitHub name stays as the username that was used on creation).

---
*Stored at `docs/12-lazy-provisioning-social-login.md` for reference.*
