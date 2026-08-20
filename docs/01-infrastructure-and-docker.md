# Phase 1: Infrastructure Containerization & Docker Networking

## 1. Architectural Motivation & Problem Statement (Why Containerization?)

In traditional monolithic systems, the application relies on a single relational database (e.g., PostgreSQL or MySQL). However, in a **Microservices Architecture**, each microservice manages its own isolated database (**Database-per-Service Pattern**). This guarantees service autonomy, independent scaling, and enables **Polyglot Persistence**—choosing the optimal database engine for each specific data access pattern:

- **`identity-service`**: MySQL (Relational DB - High ACID compliance for user accounts and credentials).
- **`post-service`**: MongoDB (Document DB - Flexible schema for blog posts and rich content).
- **`profile-service`**: Neo4j (Graph DB - Optimized graph traversal for follower/friend relationships).
- **`notification-service`**: MongoDB (Document DB - Notification history logs).
- **`chat-service`**: MongoDB (Document DB - Chat message logs).
- **Shared Infrastructure Services**:
  - **Redis**: Distributed caching and API Gateway rate limiting.
  - **Apache Kafka**: Asynchronous event broker for inter-service communication.
  - **Keycloak**: OAuth2.0 / OpenID Connect (OIDC) Identity Provider.
  - **Zipkin**: Distributed tracing dashboard.

### Challenges Without Containerization:
1. **"Works on My Machine" Syndrome**: Each developer manually installs MySQL 8, MongoDB 7, Neo4j 5, Redis 7, and Kafka 3.8 on local machines with conflicting ports and environment configurations.
2. **Version Incompatibilities**: Service A requiring MySQL 5.7 vs Service B requiring MySQL 8.0.
3. **Complex Networking**: Services communicating via hardcoded `127.0.0.1` loopbacks or static IP addresses.

---

## 2. Under-the-Hood Mechanics

### A. Docker Container vs Virtual Machine (VM)
- **VM**: Hardware-level virtualization managed by a Hypervisor. Each VM packages a full Guest OS, consuming substantial RAM/CPU and incurring slow boot times.
- **Docker Container**: OS-level virtualization. All containers share the host Linux Kernel using core kernel primitives:
  - **Linux Namespaces**: Provides process isolation (`PID`), network isolation (`NET`), inter-process communication isolation (`IPC`), mount isolation (`MNT`), hostname isolation (`UTS`), and user isolation (`USER`). Container A cannot view or access Container B's processes or network interfaces.
  - **Control Groups (cgroups)**: Enforces resource limits and metering (CPU, Memory, Disk I/O) allocated to each container.

### B. Docker Compose Networking & Internal DNS Resolution
When services are defined within a shared custom bridge network (e.g., `mini-blog-network`):

1. **Embedded DNS Server**: Docker runs an internal DNS server at `127.0.0.11`.
2. **Container Name Resolution**: 
   - When `identity-service` queries `mysql:3306`, Docker's DNS server resolves `mysql` to its internal container IP (e.g., `172.28.0.2`).
   - Consequently, application configuration files use container hostnames (`jdbc:mysql://mysql:3306/identity-service`) rather than volatile IP addresses or `localhost`.

### C. Persistent Storage (Docker Volumes)
By default, a container's filesystem is **ephemeral**—data created inside the container is destroyed when the container stops or is recreated.
- **Named Volumes**: Docker manages persistent storage locations on the Host OS (e.g., `/var/lib/docker/volumes/...`).
- Mounting a named volume (`mysql_data:/var/lib/mysql`) guarantees that data persists across container restarts, upgrades, and recreations.

---

## 3. Infrastructure Topology (`docker-compose.yml`)

| Service Name | Container Image | Internal Port | External Port (Host) | Core Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `mysql` | `mysql:8.0` | 3306 | 3306 | Relational DB for Identity Service |
| `mongodb` | `mongo:7.0` | 27017 | 27017 | Document DB for Post, Notification, and Chat Services |
| `neo4j` | `neo4j:5.15-community` | 7474, 7687 | 7474, 7687 | Graph DB for Profile Service |
| `redis` | `redis:7.2-alpine` | 6379 | 6379 | Distributed Caching & Gateway Rate Limiting |
| `kafka` | `bitnami/kafka:3.8` | 9092 (Internal), 9094 (External) | 9094 | KRaft Mode Event Broker |
| `akhq` | `tchiotludo/akhq:0.24.0` | 8080 | 8090 | Kafka Management Web UI |
| `keycloak` | `quay.io/keycloak/keycloak:24.0` | 8080 | 8484 | Identity Provider / OAuth2 OIDC Server |
| `zipkin` | `openzipkin/zipkin:3.4` | 9411 | 9411 | Distributed Tracing Dashboard |

---

## 4. Architectural Best Practices

1. **Explicit Custom Networks**: Always define an explicit bridge network (`driver: bridge`) to prevent containers from joining the unmanaged default bridge.
2. **Environment Parameterization**: Declare sensible default credentials and ports while enabling overrides via external `.env` files.
3. **Healthchecks & Readiness Ordering**: Utilize `healthcheck` definitions to ensure databases and brokers are fully ready before dependent applications boot.
4. **Secrets Management**: Exclude real passwords from version control by committing a `.env.example` template and keeping `.env` in `.gitignore`.

---
*Stored at `docs/01-infrastructure-and-docker.md` for reference.*
