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

## gRPC
`post-service` calls `profile-service` over gRPC. The contract lives in `profile.proto`, **duplicated** in `post-service/src/main/proto/` and `profile-service/src/main/proto/` — keep both copies in sync when changing it.

## Events
`NotificationEvent` is **copied** into both `identity-service` and `notification-service` (package `com.htphatz.event`, no shared library). Keep the two copies in sync. Event types and their Kafka topics are defined in identity-service's `EventType` enum.
