# Master Roadmap & Microservices Learning Plan: `mini-blog` Project

## 1. Executive Summary & Learning Philosophy

This document outlines the strategic roadmap for upgrading the **`mini-blog`** application into an enterprise-grade Microservices architecture, alongside a rigorous learning framework.

### Core Learning Methodology
> **LEARN -> UNDERSTAND -> DEEPLY UNDERSTAND WHY -> HANDS-ON IMPLEMENTATION -> FAULT SIMULATION & VERIFICATION**

Instead of copy-pasting sample code or superficial implementation, every microservices pattern and technology in this project is explored through three foundational questions:
1. **What specific problem in distributed systems does this technology solve?** (Issues that monolithic architecture or basic HTTP setups fail to address).
2. **What is the underlying low-level mechanism?** (e.g., Binary Serialization, Non-blocking Event Loops, Consensus Algorithms, Token Bucket Rate Limiting, Sliding Window Metrics).
3. **Why choose this specific solution over alternatives (Trade-offs & Architectural Decisions)?** (e.g., Eureka vs Consul, REST Feign vs gRPC, 2PC vs Saga Pattern, Outbox Polling vs Change Data Capture).

---

## 2. Documentation Directory Structure (`docs/`)

All theoretical foundations, architectural diagrams, deep-dive mechanics, trade-off analyses, and best practices are documented in the `docs/` directory:

- [00-microservices-learning-plan.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/00-microservices-learning-plan.md): Master strategy, learning philosophy, phase roadmap, and verification standards.
- [01-infrastructure-and-docker.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/01-infrastructure-and-docker.md): Containerization, multi-container orchestration, networking, persistent volumes, internal DNS resolution.
- [02-service-discovery-eureka.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/02-service-discovery-eureka.md): Service Registration/Discovery mechanics, Heartbeats, Lease Expiration, Peer Awareness, Self-Preservation Mode, Client-side Load Balancing.
- [03-centralized-config-server.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/03-centralized-config-server.md): Externalized configuration, Git/Native backing stores, `@RefreshScope`, Dynamic hot-reloading with Spring Cloud Bus & Kafka.
- [04-keycloak-oauth2-gateway.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/04-keycloak-oauth2-gateway.md): Keycloak architecture, Realm/Client configuration, OAuth2.0 & OIDC Flows (Authorization Code with PKCE, Client Credentials), Centralized Gateway JWT validation, Reactive Redis Rate Limiting.
- [05-resilience4j-fault-tolerance.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/05-resilience4j-fault-tolerance.md): Cascading failure prevention, Circuit Breaker State Machine (CLOSED, OPEN, HALF_OPEN), Sliding Window Metrics, Exponential Backoff Retry, RateLimiter, Bulkhead, Fallback strategies.
- [06-grpc-vs-rest-feign.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/06-grpc-vs-rest-feign.md): HTTP/1.1 vs HTTP/2 (Multiplexing, HPACK), JSON vs Protobuf Binary Serialization, Benchmarking, Dual-communication best practices (gRPC for high-throughput internal calls, REST Feign for flexible external/UI integration).
- [07-saga-pattern-and-outbox.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/07-saga-pattern-and-outbox.md): Distributed Transactions, Dual-Write Problem, Two-Phase Commit (2PC) vs Saga Pattern, Choreography vs Orchestration, Compensating Transactions (Rollback logic), Transactional Outbox Pattern & Idempotent Consumer.
- [08-redis-distributed-caching.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/08-redis-distributed-caching.md): Cache-Aside pattern, Write-Through/Write-Behind, Cache Eviction (TTL, LRU), Mitigating Cache Avalanche, Cache Penetration, and Cache Stampede.
- [09-distributed-observability.md](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/09-distributed-observability.md): The 3 Pillars of Observability (Metrics, Logs, Traces), Micrometer Tracing, Context Propagation (TraceId, SpanId via W3C/B3 headers), Prometheus pull metrics, Grafana & Zipkin visualization dashboards.

---

## 3. Phase-by-Phase Execution Roadmap

### Phase 1: Environment & Infrastructure Consolidation (`docker-compose.yml`)
* **Focus**: Unified containerization for MySQL, MongoDB, Neo4j, Redis, Kafka, AKHQ, Keycloak, Zipkin.
* **Artifact**: `docs/01-infrastructure-and-docker.md` and updated root `docker-compose.yml`.

### Phase 2: Service Discovery (`discovery-service` with Spring Cloud Netflix Eureka)
* **Focus**: Dynamic registration, removal of hardcoded IP/port routes, client-side load balancing via Spring Cloud LoadBalancer.
* **Artifact**: `docs/02-service-discovery-eureka.md` and new `discovery-service` module.

### Phase 3: Centralized Configuration Management (`config-service`)
* **Focus**: Centralized repository for service environment profiles, hot-reloading context without application restarts.
* **Artifact**: `docs/03-centralized-config-server.md` and new `config-service` module.

### Phase 4: Enterprise Identity Provider & API Gateway Security (Keycloak + Spring Cloud Gateway)
* **Focus**: Keycloak OIDC server setup, Realm/Role management, Centralized Gateway Token Introspection / JWT Decoding, Reactive Redis Rate Limiter.
* **Artifact**: `docs/04-keycloak-oauth2-gateway.md` and updated `api-gateway` configuration.

### Phase 5: Resilience & Fault Tolerance (Resilience4j)
* **Focus**: Circuit Breaker, Retries, Bulkhead, and Graceful Fallbacks for inter-service HTTP communications via OpenFeign.
* **Artifact**: `docs/05-resilience4j-fault-tolerance.md`.

### Phase 6: High-Performance Inter-Service Communication (gRPC & Protobuf)
* **Focus**: Defining `.proto` IDL, generating gRPC Stubs, HTTP/2 multiplexing for `post-service` -> `profile-service` high-frequency calls, while retaining OpenFeign REST for flexible business operations.
* **Artifact**: `docs/06-grpc-vs-rest-feign.md`.

### Phase 7: Distributed Transactions (Saga Pattern) & Reliable Messaging (Transactional Outbox)
* **Focus**: Solving Dual-Write & Eventual Consistency across microservices using Transactional Outbox table + Kafka, implementing Compensating Transactions for multi-step registration (Identity -> Profile -> Notification).
* **Artifact**: `docs/07-saga-pattern-and-outbox.md`.

### Phase 8: Distributed Caching Layer (Spring Cache + Redis)
* **Focus**: Cache-Aside implementation for post feeds and user profiles, configuring TTL, cache eviction, and distributed locks to prevent Cache Stampede.
* **Artifact**: `docs/08-redis-distributed-caching.md`.

### Phase 9: Distributed Observability & Monitoring (Zipkin, Prometheus, Grafana)
* **Focus**: Context propagation tracing across HTTP & Kafka, Micrometer instrumentation, Prometheus metrics collection, Grafana operational dashboards.
* **Artifact**: `docs/09-distributed-observability.md`.

---

## 4. Verification & Quality Assurance Standards

1. **Failure Injection Testing**: Intentionally simulate network partition, high latency, service crashes, database outages, and expired tokens to verify system resilience.
2. **Production-Ready Code Standards**: Ensure clean architecture, clear layer separation, proper error handling, structured logging, and non-blocking I/O where appropriate.

---
*Stored at `docs/00-microservices-learning-plan.md` for reference and review.*
