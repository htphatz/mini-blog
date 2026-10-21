# Phase 13: Event-Driven Registration (Keycloak Event Listener → Kafka → Choreography Saga)

## 1. Architectural Motivation (Why Events From Keycloak?)

`AuthService.register()` in `identity-service` is an **orchestration Saga**: create the user → create the profile → (only when the profile succeeded) write an Outbox row → Kafka `welcome-blog` → `notification-service` sends the welcome email (Phase 7).

### The Problem: Registration Through Keycloak Bypasses the Saga
A user who registers on the Keycloak Register page, or logs in with GitHub for the first time, never goes through `register()`. Such a user only gets a lazily created profile through `GET /api/v1/profile/me` (Phase 12) and **no welcome email**. The registration moment is also not reflected anywhere in the system until the user calls an API.

### The Solution: Keycloak Publishes a `REGISTER` Event to Kafka
A Keycloak event listener publishes the `REGISTER` event to Kafka. `profile-service` reacts to it by creating the profile and then emitting the event that triggers the welcome email. `identity-service` is **not changed**.

```
Keycloak (REGISTER) ──plugin keycloak-kafka──► topic `keycloak-events`
   └─► profile-service (KeycloakEventConsumer)
         1. createProfile(userId, ...)            ← idempotent by userId
         2. only on success: publish NotificationEvent
                                              ──► topic `welcome-blog`
                                                   └─► notification-service (unchanged): sends the email via Brevo
```

---

## 2. Under-the-Hood Mechanics

### A. Keycloak Event Listener
Keycloak emits an event for each action (`REGISTER`, `LOGIN`, `LOGOUT`, ...) and passes it to every **event listener** enabled in the realm (*Realm settings → Events → Event listeners*). The built-in listeners only log or send emails. The `keycloak-kafka` plugin adds a listener named `kafka` that serializes the event to JSON and produces it to a Kafka topic.

The `kafka` listener does nothing until it is added to the realm's *Event listeners* list, even when its environment variables are set.

### B. What the Plugin Variables Mean

| Variable | Role |
| :--- | :--- |
| `KAFKA_BOOTSTRAP_SERVERS: kafka:9092` | **Address of the broker to connect to** (not the destination of events). `kafka` is the container name in the Docker network and `9092` is the internal listener. From the host (for example a service running outside Docker) the address is `localhost:9094` |
| `KAFKA_TOPIC: keycloak-events` | **Where events are written**. All published events go to this topic |
| `KAFKA_CLIENT_ID: keycloak` | Identifies the producer in Kafka logs and metrics. It does not affect delivery |
| `KAFKA_EVENTS: REGISTER` | **Filter by event type.** Only `REGISTER` is published; `LOGIN`, `LOGOUT`, ... are ignored |

### C. Choreography vs Orchestration

| | `identity-service` `register()` | Keycloak → Kafka |
| :--- | :--- | :--- |
| Saga style | Orchestration (one place drives the flow) | Choreography (services react to events) |
| Reliable event publishing | Transactional Outbox | No Outbox (see limitations) |
| Compensation on failure | Delete the Keycloak user | Kafka retry (every step is idempotent) |
| Email after profile | Yes | Yes (`profile-service` emits `welcome-blog` only after the profile is created) |

`identity-service` creates users through the Admin API, which does not emit a `REGISTER` event, so a user is never provisioned twice.

### D. Idempotency and Ordering
- **Profile first, email second.** The welcome event is published only after the profile exists, the same ordering rule as the Saga in Phase 7.
- `ProfileService.createProfile` is keyed by `userId`, so a redelivered event cannot create a second profile.
- The email `eventId` is deterministic (`"welcome-" + userId`), so the idempotent consumer in `notification-service` skips duplicates (`Duplicate event detected`).
- A failure (for example Neo4j is down) throws an exception from the listener, and Spring Kafka retries the record. The send to `welcome-blog` uses `join()` so that an asynchronous send failure also throws and is retried instead of being lost.

### E. The Event Payload
The plugin README does not document the JSON, so it was verified from a real message (GitHub first login):

```json
{
  "id": "<event-uuid>",
  "time": 1792553236521,
  "type": "REGISTER",
  "realmId": "<realm-uuid>",
  "clientId": "mini-blog-gateway",
  "userId": "<keycloak-user-uuid>",
  "sessionId": null,
  "ipAddress": "172.19.0.1",
  "error": null,
  "details": {
    "identity_provider": "github",
    "register_method": "broker",
    "identity_provider_identity": "<github-username>",
    "code_id": "<uuid>",
    "email": "user@example.com",
    "username": "<github-username>"
  }
}
```

- A first GitHub login **does** emit `REGISTER` (`register_method: broker`) with `email` and `username`.
- `first_name` / `last_name` are **absent** for GitHub because the provider does not supply them at that step, so the consumer falls back to `username` for the first name. For the Keycloak Register form, `first_name` and `last_name` are expected but have not been verified.

---

## 3. Implementation Blueprint in `mini-blog`

### A. Keycloak Side

1. **Plugin JAR.** Download `keycloak-kafka-1.3.0-jar-with-dependencies.jar` into `keycloak/providers/` (see `keycloak/providers/README.md`; JARs are git-ignored).
2. **`docker-compose.yml`, service `keycloak`:** the volume `./keycloak/providers:/opt/keycloak/providers` and the variables `KAFKA_TOPIC`, `KAFKA_CLIENT_ID`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_EVENTS`, plus `depends_on: kafka`. `start-dev` rebuilds Keycloak with the provider on startup.
3. **Recreate the container:** `docker compose up -d keycloak`, then check the log for `Init kafka module ...`.
4. **Admin Console, realm `mini-blog-realm`:**
   - Realm settings → Events → **Event listeners**: add `kafka` (keep `jboss-logging`).
   - Realm settings → Login → **User registration: ON**; leave **Verify email: OFF** while testing (no SMTP is configured).
   - Realm settings → User profile: make `firstName` and `lastName` required.
   - Realm settings → User registration → Default roles → `USER` (recommended).

### B. `profile-service`
- `pom.xml`: `spring-kafka`.
- `application.yml`: `spring.kafka` with consumer group `profile-group`, `auto-offset-reset: earliest`, `StringDeserializer` for key and value (the Keycloak JSON is parsed manually so no JSON type headers are needed), and producer `StringSerializer` + `JsonSerializer`.
- `dto/event/KeycloakEvent`: minimal view of the event (`type`, `userId`, `details`), unknown fields ignored.
- `service/KeycloakEventConsumer`: `@KafkaListener(topics = "keycloak-events", groupId = "profile-group")`
  1. Parse the JSON; a malformed message is logged and skipped (retrying cannot fix it).
  2. Ignore everything except `REGISTER`.
  3. `firstName` = `details.first_name` → `details.username` → the local part of the email; `lastName` = `details.last_name`.
  4. `ProfileService.createProfile` (idempotent).
  5. If `details.email` is present, publish a `NotificationEvent` (`eventId = "welcome-" + userId`) to `welcome-blog`; otherwise log and skip the email.
- `com.htphatz.event.NotificationEvent`: the **third copy** (with `identity-service` and `notification-service`). Keep them in sync and in the same package because the JSON type header carries the fully qualified class name (`__TypeId__`).

### C. Verification
1. `docker logs keycloak` shows no plugin error; the provider `kafka` is in *Event listeners*.
2. Register a new user on the Register page (or log in with a new GitHub account).
3. AKHQ (`localhost:8085`) → `keycloak-events`: a `REGISTER` message exists. Compare the `details` keys with §2.E.
4. Neo4j has exactly one `profile` node with `userId = sub`; AKHQ → `welcome-blog` has `eventId = welcome-<userId>`; `notification-service` logs `Processing welcome-blog event` and the email is sent.
5. Check that the consumer kept up:
   ```bash
   docker exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group profile-group
   ```
   `LAG` must be `0`.
6. Idempotency: produce the same message to `keycloak-events` again: no second profile, and `notification-service` logs `Duplicate event detected`.
7. Regression: `POST /api/v1/identity/auth/register` still runs the old Saga and does not produce a `REGISTER` message.

Verified so far with a first GitHub login: the `REGISTER` event, the profile consumer lag `0`, and the `welcome-blog` message with the correct `__TypeId__` header; `notification-service` received it and called Brevo (the first attempt failed with `401` because of the API key, see D). Not verified yet: steps 6-7 and the Register-form variant.

### D. Troubleshooting

| Symptom | Likely cause |
| :--- | :--- |
| Keycloak fails to start with `UnsupportedClassVersionError ... class file version 65.0` | Wrong plugin version: 1.4.0 is compiled for Java 21 but Keycloak 24.0 runs Java 17. Use 1.3.0 |
| No Register link on the login page | *User registration* is off |
| No message in `keycloak-events` | `kafka` is not in *Event listeners*, or Keycloak cannot reach `kafka:9092` |
| Message present but no profile | `profile-service` is not running, or the `details` keys differ from §2.E |
| `notification-service` fails with `401 Unauthorized` from `api.brevo.com` | Invalid `BREVO_API_KEY` (use the v3 API key `xkeysib-...`, not the SMTP key, and check Brevo's authorized IPs). After retries are exhausted the record is skipped; republish the `welcome-blog` message to resend |

---

## 4. Trade-Off Analysis: Ways to React to a Keycloak Registration

| Approach | Strength | Weakness |
| :--- | :--- | :--- |
| **Off-the-shelf Kafka plugin + consumer in `profile-service` (chosen)** | No Java extension to write, fits the existing Kafka stack | Third-party JAR, version must match the Keycloak Java runtime, payload not documented |
| Custom Keycloak event listener SPI | Full control over payload and delivery | A separate Maven project deployed into Keycloak, must be maintained |
| Webhook plugin calling `profile-service` over HTTP | No Kafka needed | `profile-service` binds `127.0.0.1` (Phase 10) so a Keycloak container cannot reach it, and the internal endpoint is `permitAll` |
| CDC with Debezium on the Keycloak database | No Keycloak plugin | Heavy infrastructure, reads internal tables |
| Lazy only (Phase 12) | No infrastructure | Not immediate, no welcome email |

Lazy `GET /profile/me` stays as the fallback for every approach.

---

## 5. Known Limitations

- **No Outbox.** If `profile-service` creates the profile but publishing `welcome-blog` fails, the record is retried (idempotent); when retries are exhausted the email can be lost while the profile stays correct.
- **Events can be lost** if Kafka is unreachable while a user registers. Fallback: `GET /profile/me` creates the profile lazily, but no email is sent. Plugin 1.4.0 added a fix to keep Keycloak available when Kafka is down (PR #62), but the Java 17 compatible version in use is **1.3.0**, which does not include it. Behavior with Kafka down has **not been tested**; a producer timeout such as `KAFKA_MAX_BLOCK_MS` can bound the wait.
- **The plugin version must target Java 17.** Re-check `docker logs keycloak` after any Keycloak or plugin upgrade; the plugin README only lists Keycloak 19/21. If a later version stops working, write a small event listener SPI instead.
- **First GitHub login supplies no first or last name**, so the profile (and the welcome email) use the GitHub username.
- The welcome email is sent only when the event has an `email`.
- Delivery on the consumer side is at-least-once; after the retries are exhausted a failed record is skipped (a dead-letter topic would be the next step).

---
*Stored at `docs/13-keycloak-kafka-register-events.md` for reference.*
