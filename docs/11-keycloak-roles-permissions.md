# Phase 11: Authorization — Mapping Keycloak Roles & Permissions into Spring Security (JWT → GrantedAuthority → `@PreAuthorize`)

> **Goal:** understand and configure the chain *Keycloak role → JWT claim → `GrantedAuthority` → `@PreAuthorize`*.
> **Repo status:** this model is applied in **`post-service`** (see [§4.D](#d-applied-in-post-service)). The other services still read realm roles only and have no `@PreAuthorize`. The snippets in §4.A–C are learning templates.
> **Related:** [`../KEYCLOAK.md`](../KEYCLOAK.md) (B2, K-4, K-5, K-17, K-18, K-19, K-22), [`04-keycloak-oauth2-gateway.md`](04-keycloak-oauth2-gateway.md).
> **Legend:** ✅ read directly in the code · 🔎 standard Keycloak/Spring behavior, not yet exercised in this repo.

## Table of Contents
1. [Architectural Motivation (Why Role/Permission Mapping?)](#1-architectural-motivation-why-rolepermission-mapping)
2. [Under-the-Hood Mechanics](#2-under-the-hood-mechanics)
3. [Keycloak Setup & Configuration Blueprint](#3-keycloak-setup--configuration-blueprint)
4. [Implementation Blueprint in `mini-blog`](#4-implementation-blueprint-in-mini-blog)
5. [Common Pitfalls](#5-common-pitfalls)
6. [Verification Lab](#6-verification-lab)
7. [Trade-Off Analysis](#7-trade-off-analysis-role-models-and-group-assignment)
8. [Remaining Work](#8-remaining-work)

---

## 1. Architectural Motivation (Why Role/Permission Mapping?)

Authentication (Phase 4) proves *who* a caller is. Authorization decides *what* that caller may do. In `mini-blog` every service is its own resource server, so each one must turn the claims in the JWT into Spring Security authorities and then enforce rules on them.

### The Problem: Roles Exist in Keycloak but Are Barely Used in Code

| Stage | State in the repo |
| :--- | :--- |
| Assign role at registration | ✅ `AuthService.java:115-129`: tries role `ROLE_USER`, then `USER`, then only `log.warn` |
| Claim in the JWT | ✅ Only `realm_access.roles` is read |
| Converter | ✅ `KeycloakRoleConverter.java:19-34`: adds the `ROLE_` prefix if missing; duplicated in 6 places (identity, profile, post, notification, chat, gateway) |
| Method security enabled | ✅ `@EnableMethodSecurity` in identity, profile, post, notification, chat (`WebSecurityConfig`). The gateway is WebFlux so it has none |
| Used in code | ❌ No `@PreAuthorize`/`hasRole`/`hasAuthority`; `ProfileService.java:15` imports `PreAuthorize` but does not use it |
| Ownership | ❌ `PostService.updatePost` / `deletePost` do not compare `userId` (K-5) |

The "bring roles into the code" half is partly done. What remains is to **configure roles in Keycloak completely** and **enforce them where access must be restricted**.

### The Solution: Keycloak Roles → JWT Claims → Spring Authorities

```
Keycloak                      JWT (access token)                 Service (resource server)
┌───────────────┐   issues     ┌───────────────────────────┐   ┌──────────────────────────────────┐
│ user ─ role   │ ───────────► │ realm_access.roles        │──►│ KeycloakRoleConverter            │
│ client role   │   token      │ resource_access.<c>.roles │   │   → GrantedAuthority (ROLE_*)    │
│ composite     │              └───────────────────────────┘   │ → @PreAuthorize / hasRole        │
└───────────────┘                                              └──────────────────────────────────┘
```

---

## 2. Under-the-Hood Mechanics

### A. Role vs Permission vs Ownership

Keycloak has no simple "permission" object like the one used in code. There are several ways to represent one:

| Model | Claim in the JWT | Use when | Complexity |
| :--- | :--- | :--- | :--- |
| **Realm role** (`USER`, `ADMIN`) | `realm_access.roles` | Coarse permissions shared by all services | Low |
| **Client role** (client `post-service-client`: `post:delete`) | `resource_access.post-service-client.roles` | Per-service permissions, avoids name clashes between services | Low |
| **Composite role** | Child roles are "expanded" into the token | Bundle permissions: `ADMIN` = `post:delete` + `post:update` | Low |
| **Authorization Services** (UMA: resource, scope, policy) | `authorization.permissions` (RPT only) | Per-resource permissions decided by Keycloak | High, rarely needed |

Terms that must be kept apart:

| Term | Meaning | Example |
| :--- | :--- | :--- |
| Role | "Who you are" (a function) | `ADMIN` |
| Permission / authority | "What you may do" (an action) | `post:delete` |
| Ownership | "Whose resource this is" | `post.userId == sub` |

### B. How Roles Reach the Token

A role appears in the JWT only when:
- The client that requests the token (`mini-blog-gateway`, `identity-service-client`) has the client scope **`roles`** (attached by default); that scope contains the *realm roles* and *client roles* mappers.
- The mapper has **Add to access token = On**.
- The client has **Full scope allowed = On** (default), or a *Scope → Assign role* entry for the required role.

Sample JWT for reference:

```json
{
  "iss": "http://localhost:8484/realms/mini-blog-realm",
  "sub": "3b2a...-uuid",
  "preferred_username": "an@example.com",
  "realm_access": { "roles": ["default-roles-mini-blog-realm", "USER", "ADMIN"] },
  "resource_access": {
    "post-service-client": { "roles": ["post:delete", "post:update"] },
    "account": { "roles": ["manage-account", "view-profile"] }
  }
}
```

Frequently misunderstood points:
- The key inside `resource_access` is the **client that owns the role**, not the client that requested the token. A token issued to `mini-blog-gateway` still carries `resource_access.post-service-client` if the user was assigned that role.
- Composite roles are **expanded in advance**: assigning `ADMIN` puts `post:delete` into the token as well, so the code does not compute it.

### C. From Claim to Authority: `hasRole` vs `hasAuthority`

| Expression | Matches authority | With the sample converter |
| :--- | :--- | :--- |
| `hasRole('ADMIN')` | `ROLE_ADMIN` | Realm role `ADMIN` |
| `hasAuthority('ROLE_ADMIN')` | `ROLE_ADMIN` | Same meaning as the line above |
| `hasAuthority('post:delete')` | `post:delete` | Client role `post:delete` |
| `hasRole('post:delete')` | `ROLE_post:delete` | **Wrong**: no such authority exists |

### D. Who Is Responsible for What

| Question | Answered by |
| :--- | :--- |
| "What role does this user have?" | **Keycloak** (roles, composites, default roles) |
| "Which APIs may this role call?" | **Code** (`@PreAuthorize`, `authorizeHttpRequests`) |
| "Whose post is this?" | **Code + database** (ownership); Keycloak does not know |

Do not push ownership into Keycloak: it is business data, changes constantly, and lives in each service's database.

---

## 3. Keycloak Setup & Configuration Blueprint

Assumes realm `mini-blog-realm`. Menu names follow Keycloak 24 🔎.

### A. Admin Console Steps

#### Step 1: Create realm roles
*Realm roles → Create role*: `USER`, `ADMIN`. Pick **one** naming convention (K-19): use `USER`/`ADMIN` without a prefix, because the converter adds `ROLE_` itself.

#### Step 2: Create permissions as client roles (optional)
*Clients → `post-service-client` → Roles → Create role*: `post:create`, `post:update`, `post:delete`.

> `post-service-client` is only a "container" for roles; it is not used to log in. If it does not exist, create it with *Clients → Create client* (Client ID `post-service-client`, all flows off).

#### Step 3: Bundle with composite roles
*Realm roles → `ADMIN` → **Associated roles** tab → Assign role* → *Filter by clients* → choose `post:delete`, `post:update`. Anyone with `ADMIN` now has both permissions.

#### Step 4: Assign to users
*Users → select a user → **Role mapping** → Assign role*.
To give new users `USER` automatically: *Realm settings → User registration → **Default roles** → Assign role*. The role assignment block in `AuthService.java:115-129` is then only a fallback.

#### Step 5: Make sure the role reaches the token
See §2.B (client scope `roles`, mapper *Add to access token*, *Full scope allowed*).

### B. Granting Permissions Through Groups

#### Why groups
Assigning roles per user is tedious. A group is a set of users: attach client roles to the group once and every member has them. Group permissions still end up in the token because Keycloak computes **effective roles** (direct roles + roles from groups + composites).

```
Group "users" ── role mapping ──► post-service-client: post:create, post:update, ...
     ▲
 users A, B, C (members)        ⇒ their tokens contain resource_access.post-service-client.roles
```

#### Configure the group (one time)
1. *Groups → Create group* → `users`.
2. *Groups → `users` → **Role mapping** tab → Assign role → Filter by clients* → choose `post:create`, `post:update`, ... of `post-service-client`; and `identity:*` of `identity-service-client` if needed.
3. Verify: *Users → a user → **Groups** tab* must show `users`.

#### Automatically add new users to `users` (Default groups)
Recommended: use the built-in Keycloak feature, **no code change**.

1. *Realm settings → **User registration** → **Default groups** tab → Add groups* → choose `users` → *Add*.
2. From now on **every new user** joins group `users`, including users created through the Admin API (`AuthService.register` calls `POST /admin/realms/{realm}/users`), through the registration page, or by hand 🔎. This also covers users created by social login (see Phase 12).
3. Users that already exist are **not** added: use *Users → user → Groups → Join Group → `users`*.

Verification: register a new user with `POST /api/v1/identity/auth/register`, open *Users → user → Groups* and see `users`; log in again, decode the token, and it must contain `resource_access.post-service-client.roles`.

---

## 4. Implementation Blueprint in `mini-blog`

> A and B are learning templates. When applying them, note that the converter is duplicated in 6 places (K-4), so change them in sync or consolidate.

### A. Extend the Converter to Read Client Roles Too

The converter currently reads realm roles only. The extended version:

```java
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final String clientId;                       // for example "post-service-client"

    public KeycloakRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Realm role: ADMIN -> ROLE_ADMIN (use with hasRole)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }

        // 2. Client role = permission: post:delete -> post:delete (use with hasAuthority)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null
                && resourceAccess.get(clientId) instanceof Map<?, ?> client
                && client.get("roles") instanceof List<?> perms) {
            perms.forEach(p -> authorities.add(new SimpleGrantedAuthority(p.toString())));
        }
        return authorities;
    }
}
```

Attach it in `WebSecurityConfig` (keep the current approach in `jwtAuthenticationConverter()`):

```java
jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter("post-service-client"));
```

### B. Use It in Controllers / Services

`@EnableMethodSecurity` is already on in the services, so only annotations are needed:

```java
// By role
@PreAuthorize("hasRole('ADMIN')")
public List<UserResponse> getAllUsers() { ... }

// By permission
@PreAuthorize("hasAuthority('post:delete')")
public void deletePost(String id) { ... }

// Ownership: Keycloak does not know whose post it is, so check it in code
@PreAuthorize("hasRole('ADMIN') or @postSecurity.isOwner(#id, authentication)")
public PostResponse updatePost(String id, PostRequest request) { ... }
```

The ownership bean:

```java
@Component("postSecurity")
@RequiredArgsConstructor
public class PostSecurity {
    private final PostRepository postRepository;

    public boolean isOwner(String postId, Authentication authentication) {
        return postRepository.findById(postId)
                .map(post -> post.getUserId().equals(authentication.getName())) // name = sub
                .orElse(false);
    }
}
```

`authentication.getName()` is the Keycloak `sub`, which is also the `userId` the services already use (see `PostService.java:45`).

### C. URL Rules (Instead of Annotations)

```java
httpSecurity.authorizeHttpRequests(request -> request
        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
        .requestMatchers(HttpMethod.DELETE, "/posts/**").hasAuthority("post:delete")
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated());
```

Use URL rules for coarse authorization; use `@PreAuthorize` when the decision depends on data (ownership).

### D. Applied in `post-service`

The real project configuration: permissions are **client roles** of the client `post-service-client`, granted through **group `users`** (no realm roles). A real token looks like:

```json
{
  "aud": ["post-service-client", "identity-service-client"],
  "azp": "mini-blog-gateway",
  "resource_access": {
    "post-service-client":     { "roles": ["post:update", "post:create"] },
    "identity-service-client": { "roles": ["identity:update", "identity:create"] }
  }
}
```

Note: there is **no `realm_access`**, and the key in `resource_access` is `post-service-client` (not `post-service`).

#### Code changes

| File | Change |
| :--- | :--- |
| `config/KeycloakRoleConverter.java` | Takes a `clientId`; besides realm roles (`ROLE_*`, if present) it also reads `resource_access.<clientId>.roles` as verbatim authorities. A missing `realm_access` is handled |
| `config/WebSecurityConfig.java` | `clientId` comes from `${keycloak.client-id}` and is passed to the converter |
| `application.yml` | `keycloak.client-id: post-service-client` (same key name as `identity-service/application.yml`) |
| `service/PostService.java` | `@PreAuthorize("hasAuthority('post:create' / 'post:update' / 'post:delete')")` |
| `exception/GlobalExceptionHandler.java` | `AccessDeniedException` → 403 (it used to become 500) |

> **A bug that was hit:** initially `clientId` came from `spring.application.name` (= `post-service`), so the converter looked up `resource_access.post-service`, found nothing, and every `hasAuthority('post:*')` returned 403. Verified with the claims of a real token: `post-service-client` → `[post:update, post:create]`, `post-service` → `[]`.

Naming convention: **Client ID = `<service>-client`** (`post-service-client`, `identity-service-client`), set in each service's `keycloak.client-id`. Renaming a client in Keycloak means changing this property too.

#### Current authorization rules

| Action | Condition |
| :--- | :--- |
| List / view posts | Authenticated only |
| Create a post | `post:create` |
| Update a post | `post:update` |
| Delete a post | `post:delete` |

There is currently **no ownership check** (a user with `post:update` can edit someone else's post) and no ADMIN exception. Ownership remains open work (K-5); see the `postSecurity.isOwner` template in §4.B.

#### Test matrix (sample token: has `post:create`, `post:update`, lacks `post:delete`)

| Action | Result |
| :--- | :--- |
| Create a post | 200 |
| Update a post | 200 |
| Delete a post | **403** (missing `post:delete`) |
| View posts | 200 |
| No token | 401 |

#### Notes
- **Log in again** after changing groups/roles: an old token does not update itself.
- Only `post-service` was changed. Comments and reactions have no `@PreAuthorize` yet; `deletePost` also has a pre-existing bug where `reactionRepository.deleteById(id)` uses the post id instead of `reaction.getId()` (not fixed).

---

## 5. Common Pitfalls

| Symptom | Cause | Fix |
| :--- | :--- | :--- |
| Role exists in Keycloak but is missing from the token | Client scope `roles` not attached; *Full scope allowed* off; mapper has *Add to access token* off | Check §2.B |
| Still denied after changing a role | JWT is stateless: roles only change when a **new token** is issued | Log in again / refresh; or wait for `exp` |
| `hasRole('ADMIN')` is always false | `ROLE_` mismatch (see §2.C) or the token has no role | Decode the token at jwt.io and compare |
| Messy role names | `AuthService` tries both `ROLE_USER` and `USER` (K-19) | Settle on `USER`, remove the other branch |
| Access denied returns **500** instead of **403** | `GlobalExceptionHandler` has `@ExceptionHandler(RuntimeException.class)` returning 500 (✅ `post-service/.../GlobalExceptionHandler.java:43-51`), and `AccessDeniedException` is a `RuntimeException`, so the 403 is swallowed | Add a dedicated handler (below; already added in `post-service`, line 82) |
| `@PreAuthorize` has no effect | Self-invocation, or a non-`public` method / a bean that is not proxied | Put the annotation on a bean that is called from outside |
| `/api/v1/auth/me` at the gateway shows no roles | The cookie path uses `OidcUser` and does not go through `KeycloakRoleConverter` (K-22). Services receive a Bearer JWT so they are still correct 🔎 | Affects the gateway only |
| Token has no `resource_access` | The group has no role mapping, the user is not in the group, or the client requesting the token has *Full scope allowed* off without a scope for the role | Check §3.B |
| Still 403 right after assigning a group | The user has not logged in again to get a new token | Log in again |
| Every `hasAuthority('post:*')` is 403 even though the token has the role | Wrong `keycloak.client-id`: it does not match the key in `resource_access` (for example `post-service` instead of `post-service-client`) | Fix the property |
| A newly registered user has no permissions | Default groups not configured; users only join the group from then on | Configure Default groups (§3.B) |
| Token has no `realm_access` | Normal if only client roles are used and the user has no realm role; the converter handles it | None |

Handler that returns 403 correctly (placing it alongside `RuntimeException` is fine because Spring selects the most specific matching handler):

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<APIResponse> handleAccessDenied(AccessDeniedException e) {
    APIResponse apiResponse = APIResponse.builder()
            .code(HttpStatus.FORBIDDEN.value())
            .message("Access denied")
            .build();
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse);
}
```

The other services still need to be checked for a similar handler (only `post-service` has been read).

---

## 6. Verification Lab

Requires Keycloak running and the services restarted.

1. **Configure:** do §3.A steps 1-4; give `ADMIN` to one user and `USER` to the other.
2. **Get a token** with ROPC (learning only, see `KEYCLOAK-ENDPOINTS.md` A3):
   ```bash
   curl -s -X POST http://localhost:8484/realms/mini-blog-realm/protocol/openid-connect/token \
     -d client_id=identity-service-client -d client_secret=<SECRET> \
     -d grant_type=password -d username=an@example.com -d password=<PASSWORD>
   ```
3. **Decode** the `access_token` at jwt.io (or `cut -d. -f2 | base64 -d`): you must see `realm_access.roles` (and `resource_access` if steps 2-3 were done).
4. **Call an API** protected with `@PreAuthorize("hasRole('ADMIN')")` using the `ADMIN` token ⇒ 200; with the `USER` token ⇒ 403 (or 500 if the handler from §5 is missing).
5. **Change the role** of the user in Keycloak and call again with the **old token** ⇒ no change; get a new token ⇒ it changes. This demonstrates stateless JWT.
6. **Ownership:** log in as two different users; user B tries to edit user A's post ⇒ 403; ADMIN can edit.
7. **Default groups:** register a user through `POST /api/v1/identity/auth/register`, then verify group membership and the token as described in §3.B.

---

## 7. Trade-Off Analysis: Role Models and Group Assignment

### A. Which Model for `mini-blog`?

| Option | Strength | Weakness | Verdict |
| :--- | :--- | :--- | :--- |
| Realm roles only | Simplest, shared across services | Coarse, names can clash between services | Use for broad roles (`USER`, `ADMIN`) |
| Client roles as permissions | Per-service, namespaced | More setup in Keycloak | **Use for permissions** (`post:delete`) |
| Composite roles | Bundle permissions once | Another layer to understand | Use to build `ADMIN` |
| Authorization Services (UMA) | Per-resource decisions by Keycloak | High complexity, needs RPT flow | Not used |

**Recommendation for mini-blog:** coarse realm roles (`USER`, `ADMIN`) + client roles as permissions (`post:delete`, ...) bundled with composite roles + ownership checks in code. Do not use Authorization Services.

### B. Default Groups (Keycloak) vs `setGroups` in Code

If failures should appear immediately in the saga instead of silently, add the group while creating the user in `AuthService.register`:

```java
userRepresentation.setGroups(List.of("/users"));   // group path, starts with "/"
```

If the group does not exist Keycloak may reject creating the user 🔎, in which case `register` returns `USER_CREATION_FAILED`. This is a secondary option that is **not applied**.

| | Default groups (Keycloak) | `setGroups` in code |
| :--- | :--- | :--- |
| Code change | No | Yes |
| Applies to users created elsewhere | Yes | No (only users created through this code) |
| When misconfigured | Silent: the user has no permissions and gets 403 on API calls | Fails immediately at registration |

---

## 8. Remaining Work

### Items in `KEYCLOAK.md` That This Document Helps Resolve

| Code | Problem | Use |
| :--- | :--- | :--- |
| K-4 | Roles are read but nobody uses them; converter duplicated 6 times | §4.A, §4.B |
| K-5 | User A edits/deletes B's post | §4.B (`postSecurity.isOwner`) |
| K-17 | Profile `PUT/DELETE users/{id}` does not compare `id` with `sub` | Same ownership template |
| K-18 | Every user can list every user | `@PreAuthorize("hasRole('ADMIN')")` |
| K-19 | Role names are inconsistent | §3.A step 1 |
| K-22 | Roles at the gateway through the cookie path | §5 |

### Next Steps (to roll out to the other services)
1. Settle role names (`USER`, `ADMIN`) and create them in Keycloak.
2. Add an `AccessDeniedException` → 403 handler to each service (done in `post-service`).
3. Extend `KeycloakRoleConverter` (or consolidate into one shared copy); done in `post-service`.
4. Add `@PreAuthorize` to identity `GET /users` (ADMIN) and profile `PUT/DELETE users/{id}` (profile owner or ADMIN).
5. The role-assignment block in `AuthService.java:115-129` still tries realm role `ROLE_USER`/`USER`; with the group model that role may not exist and the code only does `log.warn`. It can be removed once realm roles are definitely unused, but it has not been changed here.

---
*Stored at `docs/11-keycloak-roles-permissions.md` for reference.*
