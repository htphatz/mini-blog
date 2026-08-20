# Phase 7: Distributed Transactions (Saga Pattern) & Reliable Event-Driven Messaging (Transactional Outbox)

## 1. Architectural Motivation (The Distributed Transaction Challenge)

In a monolithic architecture, business workflows spanning multiple entities are executed inside a single local relational database transaction. If any operation fails, the database engine executes a simple `ROLLBACK`, guaranteeing **ACID** (Atomicity, Consistency, Isolation, Durability) properties.

In a **Microservices Architecture**, data is partitioned across multiple independent databases (**Database-per-Service**):
- User Account data lives in MySQL (`identity-service`).
- Graph / Follower relationships live in Neo4j (`profile-service`).
- Notifications live in MongoDB (`notification-service`).

### Why Two-Phase Commit (2PC) Fails in Microservices:
Traditional distributed transaction protocols like **2PC (XA Transactions)** require a centralized coordinator to lock database resources across services until all nodes vote to commit. 
- **Drawbacks of 2PC**:
  1. **Blocking & Latency**: Holds database row locks across network boundaries, causing extreme throughput degradation.
  2. **Single Point of Failure**: If the coordinator crashes during the commit phase, database resources remain locked indefinitely.
  3. **No Support for NoSQL**: Heterogeneous databases like MongoDB or Neo4j do not participate in 2PC XA protocols.

---

## 2. The Dual-Write Problem & Transactional Outbox Pattern

### The Dual-Write Problem
Consider an `identity-service` registering a new user:

```java
@Transactional
public void registerUser(UserRegistrationRequest request) {
    User user = userRepository.save(new User(...)); // Step 1: Save to MySQL
    kafkaTemplate.send("user-created-topic", new UserCreatedEvent(user.getId())); // Step 2: Publish Event
}
```

**What happens if the network drops or Kafka broker is down right after Step 1?**
- The MySQL transaction commits successfully, but Kafka fails to publish the event.
- `profile-service` and `notification-service` never receive the event. The system enters an **inconsistent state** (User exists in Identity DB, but has no Profile or Welcome Email).

### The Solution: Transactional Outbox Pattern

Instead of publishing directly to Kafka inside the business transaction, the service writes the event to a dedicated `outbox` table in the **same local database transaction**:

```
+-------------------------------------------------------------------------+
| Local MySQL Transaction                                                 |
|                                                                         |
|  1. INSERT INTO users (id, email) VALUES (...);                         |
|  2. INSERT INTO outbox_events (id, aggregate_type, payload) VALUES (...);|
+-------------------------------------------------------------------------+
                                    |
                                    | Commit Local DB Transaction
                                    v
                         [MySQL Outbox Table]
                                    |
            +-----------------------+-----------------------+
            | Outbox Poller / CDC                           | Debezium / Scheduled Job
            v                                               v
    +---------------------------------------------------------------+
    | Apache Kafka Broker ("user-created-topic")                    |
    +---------------------------------------------------------------+
                                    |
                                    v
                     +-----------------------------+
                     | notification-service        |
                     +-----------------------------+
```

1. **Atomic Write**: The domain entity (`users`) and event payload (`outbox_events`) are saved atomically in 1 local database transaction.
2. **Asynchronous Publishing**: An Outbox Poller thread or Change Data Capture (CDC) tool (e.g., Debezium) reads unpublished records from `outbox_events` and emits them to Kafka.
3. **At-Least-Once Delivery**: Guarantees that no event is lost, even if Kafka or the application crashes.

---

## 3. Saga Pattern Mechanics for Distributed Transactions

A **Saga** is a sequence of local transactions across multiple services. Each local transaction updates the database and publishes an event/message to trigger the next local transaction step.

### BASE Consistency Model
Sagas trade strict ACID consistency for **BASE Consistency**:
- **Basically Available**: Microservices remain available independently.
- **Soft State**: System state may change over time without user interaction during Saga execution.
- **Eventual Consistency**: The system becomes fully consistent once all Saga steps complete or roll back.

### Choreography vs Orchestration Sagas

| Dimension | Choreography Saga (Event-Driven) | Orchestration Saga (Central Coordinator) |
| :--- | :--- | :--- |
| **Control Flow** | Decentralized. Each service listens to events and decides next step | Centralized **Saga Execution Coordinator (SEC)** drives workflow |
| **Coupling** | Loose coupling via Kafka topics | Microservices are invoked by the central Orchestrator |
| **Complexity** | Easy for simple workflows (2-4 steps); hard to track complex loops | Ideal for complex enterprise workflows with many branches |
| **Cyclic Dependency Risk** | High risk of circular event dependencies | Low risk (Workflow explicitly defined in Orchestrator) |

### Compensating Transactions (Rollback Logic)

If any step in the Saga fails, the Saga must execute **Compensating Transactions** in reverse order to undo changes and restore data consistency.

#### Example Scenario: User Registration Saga

```
[Identity Service]                  [Profile Service]                [Notification Service]
        |                                   |                                   |
1. Create Identity (MySQL)                  |                                   |
   [Status: PENDING]                        |                                   |
        |                                   |                                   |
        |---- Event: IdentityCreated ------>|                                   |
        |                                   | 2. Create Profile (Neo4j)         |
        |                                   |    FAILED! (Constraint Error)     |
        |                                   |                                   |
        |<--- Event: ProfileCreationFailed -+                                   |
        |                                                                       |
3. Compensating Action:                                                         |
   Mark Identity as REJECTED / Delete Account                                   |
```

1. **Forward Steps**:
   - Step 1: `identity-service` creates User (Status: `PENDING`).
   - Step 2: `profile-service` attempts to create Graph Profile Node in Neo4j.
2. **Failure & Compensation**:
   - If `profile-service` fails (e.g., database constraint failure), it publishes `ProfileCreationFailedEvent`.
   - `identity-service` consumes this failure event and executes its **Compensating Transaction**: deleting the pending user or marking status as `REGISTRATION_FAILED`.

---

## 4. Implementation Blueprint in `mini-blog`

### A. MySQL Outbox Entity (`identity-service`)
```java
@Entity
@Table(name = "outbox_events")
@Data
@Builder
public class OutboxEvent {
    @Id
    private String id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    @Column(columnDefinition = "TEXT")
    private String payload;
    private boolean processed;
    private LocalDateTime createdAt;
}
```

### B. Outbox Publisher Poller
```java
@Component
@Slf4j
public class OutboxPublisher {

    @Autowired
    private OutboxEventRepository outboxRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedRate = 3000) // Runs every 3 seconds
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByProcessedFalse();
        for (OutboxEvent event : pendingEvents) {
            kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload());
            event.setProcessed(true);
        }
    }
}
```

### C. Idempotent Consumer Pattern (`notification-service`)
Because At-Least-Once Delivery can produce duplicate messages during network retries, consumers must implement **Deduplication**:

```java
@KafkaListener(topics = "user-created-topic", groupId = "notification-group")
public void handleUserCreated(String messagePayload) {
    UserCreatedEvent event = deserialize(messagePayload);
    
    // Check if event ID was already processed in MongoDB
    if (processedEventRepository.existsById(event.getEventId())) {
        log.warn("Duplicate event detected, skipping: {}", event.getEventId());
        return;
    }
    
    sendWelcomeEmail(event);
    processedEventRepository.save(new ProcessedEvent(event.getEventId()));
}
```

---
*Stored at `docs/07-saga-pattern-and-outbox.md` for reference.*
