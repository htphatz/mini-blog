# Phase 2: Service Discovery & Dynamic Routing (Spring Cloud Netflix Eureka)

## 1. Architectural Motivation (Why Service Discovery?)

In a monolithic application, inter-module calls occur in-memory within the same Java Virtual Machine (JVM). In a **Microservices Architecture**, services run in separate processes across containers, virtual machines, or cloud nodes.

### The Problem With Hardcoded Routing:
1. **Dynamic IP Assignment**: In containerized environments (Docker, Kubernetes), container IP addresses change unpredictably upon restart, deployment, or auto-scaling.
2. **Horizontal Auto-Scaling**: When `post-service` scales from 1 instance to 5 instances to handle high traffic, static configurations (e.g., `http://localhost:8083`) cannot balance traffic dynamically.
3. **Single Point of Failure (SPOF)**: Traditional hardware load balancers placed between internal services introduce single points of failure, network latency bottlenecks, and configuration overhead.

### The Solution: Service Registry & Discovery
Service Discovery acts as a dynamic phonebook for microservices. Services automatically register their network location (IP + Port) upon startup and query the registry to discover healthy instances of peer services.

---

## 2. Under-the-Hood Mechanics

### A. Client-Side Discovery vs Server-Side Discovery
- **Server-Side Discovery** (e.g., AWS ALB, NGINX): The client calls a centralized load balancer router, which queries a location registry and forwards the request.
- **Client-Side Discovery** (e.g., Spring Cloud Netflix Eureka + Spring Cloud LoadBalancer): The client queries the Service Registry once, caches the list of available instances locally, and performs client-side load balancing directly. This eliminates intermediate proxy hops and improves latency.

### B. Core Mechanics of Netflix Eureka

```
  +------------------+         1. Register (IP, Port)        +-------------------+
  |  identity-service| -----------------------------------> |                   |
  |  (Eureka Client) | <----------------------------------- |   Eureka Server   |
  +------------------+         2. Heartbeat (Every 30s)     | (discovery-service|
           |                                                |     Port 8761)    |
           | 3. Fetch Registry & Cache Locally              +-------------------+
           v                                                          ^
  +------------------+         4. Client-Side Load Balanced Call      |
  |   post-service   | -----------------------------------------------+
  +------------------+            "http://profile-service/users"
```

1. **Self-Registration**: Upon boot, `EurekaClient` sends a REST payload containing host, IP, port, health check URL, and metadata to `EurekaServer`.
2. **Heartbeat & Lease Management**:
   - The client sends a heartbeat every **30 seconds** (default) to renew its lease.
   - If the Eureka Server does not receive a heartbeat for **90 seconds** (lease expiration duration), it evicts the instance from its active registry.
3. **Registry Caching & Delta Fetches**:
   - Clients fetch the registry every **30 seconds** and cache it locally in memory.
   - If the Eureka Server is temporarily down, clients can still communicate using their local cached registry (High Availability).
4. **Self-Preservation Mode (AP System in CAP Theorem)**:
   - If a network partition occurs, Eureka Server might lose heartbeats from many healthy instances simultaneously.
   - Instead of evicting all instances (which would cause total service outage), Eureka enters **Self-Preservation Mode** when missed heartbeats exceed **15%**. It pauses instance eviction to protect healthy services from network split-brain scenarios.

---

## 3. Implementation Blueprint in `mini-blog`

### A. Eureka Server Module (`discovery-service`)
- **Port**: `8761`
- **Dependencies**: `spring-cloud-starter-netflix-eureka-server`
- **Main Annotation**: `@EnableEurekaServer`

### B. Eureka Clients (`api-gateway`, `identity-service`, `post-service`, etc.)
- **Dependencies**: `spring-cloud-starter-netflix-eureka-client`
- **Configuration**:
  ```yaml
  eureka:
    client:
      service-url:
        defaultZone: http://localhost:8761/eureka/
    instance:
      prefer-ip-address: true
  ```
- **Refactored OpenFeign Clients**:
  Replace hardcoded URLs (`url = "http://localhost:8081"`) with dynamic service names:
  ```java
  @FeignClient(name = "profile-service")
  public interface ProfileClient { ... }
  ```
- **Refactored API Gateway Routes**:
  Replace `uri: http://localhost:8080` with Eureka load-balanced URIs (`lb://identity-service`):
  ```yaml
  routes:
    - id: identity-service
      uri: lb://identity-service
      predicates:
        - Path=/api/v1/identity/**
  ```

---

## 4. Trade-Off Analysis: Eureka (AP) vs Consul/Etcd (CP) vs Kubernetes DNS

| Feature | Netflix Eureka | HashiCorp Consul | Kubernetes DNS / Service |
| :--- | :--- | :--- | :--- |
| **CAP Theorem Trade-Off** | **AP** (Availability / Partition Tolerance) | **CP** (Consistency / Partition Tolerance) | **CP** (etcd consensus) |
| **Consensus Algorithm** | Peer-to-Peer Replication (Eventually Consistent) | Raft Consensus Algorithm | Raft via etcd |
| **Primary Focus** | High availability; prefers stale data over no data | Strict consistency, Key-Value store, Health checks | Container orchestration native |
| **Infrastructure Overhead** | Runs as a lightweight Java/Spring Boot app | Requires standalone Consul agent binaries | Built directly into Kubernetes control plane |

---
*Stored at `docs/02-service-discovery-eureka.md` for reference.*
