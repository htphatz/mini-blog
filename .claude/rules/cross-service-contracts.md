---
paths:
  - "identity-service/**"
  - "notification-service/**"
  - "post-service/**"
  - "profile-service/**"
  - "**/*.proto"
---

# Cross-service flows and duplicated contracts

## Registration saga + outbox (identity-service)
`AuthService` register flow: create user in Keycloak (Admin API) → call profile-service to create the profile (`ProfileClient`, Feign over Eureka; no circuit breaker or fallback on this call) → only if that succeeds, save an `OutboxEvent` row in MySQL → `OutboxPublisher` polls unprocessed rows (`findByProcessedFalseOrderByCreatedAtAsc`) and publishes to Kafka → `notification-service` (`EmailService`, `@KafkaListener` on topic `welcome-blog`, group `notification-group`) sends the welcome email via Brevo. If profile creation fails, the Keycloak user is deleted as the compensating action. The outbox is deliberately written *after* the profile succeeds to avoid sending email prematurely.

## Registration through Keycloak (Register page / GitHub first login)
Users that don't go through `AuthService.register()` are handled by a choreography saga: the `keycloak-kafka` plugin (in `keycloak/providers/`, git-ignored JAR) publishes the `REGISTER` event to topic `keycloak-events` → `profile-service` (`KeycloakEventConsumer`, group `profile-group`) creates the profile (idempotent by `userId`) → only then publishes a `NotificationEvent` (`eventId = "welcome-" + userId`) to `welcome-blog` → `notification-service` sends the email. No outbox on this path. `GET /profile/me` (lazy get-or-create from the JWT) remains the fallback. See `docs/12` and `docs/13`.

## gRPC
`post-service` calls `profile-service` over gRPC. The contract lives in `profile.proto`, **duplicated** in `post-service/src/main/proto/` and `profile-service/src/main/proto/` — keep both copies in sync when changing it.

## Events
`NotificationEvent` is **copied** into `identity-service`, `notification-service` and `profile-service` (package `com.htphatz.event`, no shared library; the JSON type header carries the class name, so the package must match). Keep the three copies in sync. Event types and their Kafka topics are defined in identity-service's `EventType` enum.
