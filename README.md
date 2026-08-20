# Mini Blog - Microservices Backend System

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-6DB33F?style=flat&logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.8-231F20?style=flat&logo=apachekafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Redisson-DC382D?style=flat&logo=redis&logoColor=white)
![Resilience4j](https://img.shields.io/badge/Resilience4j-2.2.0-FF6F00?style=flat)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?style=flat&logo=mongodb&logoColor=white)
![Neo4j](https://img.shields.io/badge/Neo4j-5.15-008CC1?style=flat&logo=neo4j&logoColor=white)
![Keycloak](https://img.shields.io/badge/Keycloak-24.0-008080?style=flat&logo=keycloak&logoColor=white)

A high-performance, resilient Microservices social blogging & real-time messaging backend system built on **Java 21**, **Spring Boot 3.x**, **Spring Cloud**, **Keycloak**, **Apache Kafka (KRaft)**, **gRPC**, **Redis**, **MySQL**, **MongoDB**, **Neo4j**, **Resilience4j**, and **Zipkin**. The system is engineered using **Event-Driven Architecture (EDA)**, **Saga Pattern**, **Transactional Outbox**, and **Distributed Caching** to deliver high availability, low latency, and horizontal scalability.

---


## Table of Contents
- [1. Project Overview](#1-project-overview)
- [2. Microservices Architecture & Features](#2-microservices-architecture--features)
- [3. Tech Stack](#3-tech-stack)
- [4. System Architecture](#4-system-architecture)
- [5. Project Structure](#5-project-structure)
- [6. Authentication & Authorization](#6-authentication--authorization)
- [7. API & Communication Protocols](#7-api--communication-protocols)
- [8. Getting Started](#8-getting-started)
- [9. Docker & Infrastructure Deployment](#9-docker--infrastructure-deployment)
- [10. Technical Highlights & Design Patterns](#10-technical-highlights--design-patterns)
- [11. Detailed Architecture Documentation](#11-detailed-architecture-documentation)
- [12. Future Improvements](#12-future-improvements)

---

## 1. Project Overview

**Mini Blog** is an enterprise-grade microservices platform for content creators, social blogging, and real-time interaction:
* **Identity & Security**: Centralized authentication powered by Keycloak OIDC, RBAC, and Spring Cloud API Gateway token validation.
* **User Profile & Social Graph**: Neo4j-backed follower/following social network connections and rich user profile management.
* **Content Management & Polyglot Persistence**: MongoDB document storage for dynamic blog post content, tags, media attachments, and comments.
* **Real-time Chat & Notifications**: Kafka event streaming and WebSocket support for instant messaging and notification delivery.
* **Resilience & High Throughput**: Inter-service communication via high-performance gRPC (Protobuf over HTTP/2) and Spring Cloud OpenFeign with Resilience4j circuit breakers.

---

## 2. Microservices Architecture & Features

| Service | Primary Responsibility | Data Store / Protocol |
| :--- | :--- | :--- |
| **API Gateway** | Central entry point, Routing, Token Validation, Reactive Rate Limiting | Spring Cloud Gateway, Redis |
| **Discovery Service** | Dynamic Service Registration & Client-Side Load Balancing | Spring Cloud Netflix Eureka |
| **Config Service** | Centralized & Externalized Configuration Management | Spring Cloud Config, Git/Native |
| **Identity Service** | User Authentication, Keycloak Integration, Credentials & Token Management | Keycloak, MySQL |
| **Profile Service** | User Profiles, Social Network Connections & Relationships | Neo4j, Redis |
| **Post Service** | Blog Content, Categories, Tagging, Comments, Reaction Feed | MongoDB, Redis, gRPC |
| **Notification Service** | Async Email/Push Notifications via Kafka Event Consumers | Apache Kafka, Brevo API |
| **Chat Service** | Real-time One-on-One & Group Messaging | WebSocket, MongoDB |

---

## 3. Tech Stack

| Layer / Component | Technology / Framework |
| :--- | :--- |
| **Core Platform** | Java 21 LTS, Spring Boot 3.x, Spring Cloud 2023.x |
| **API Gateway & Routing** | Spring Cloud Gateway (Reactive WebFlux) |
| **Service Discovery & Config** | Spring Cloud Netflix Eureka, Spring Cloud Config Server |
| **Relational Database** | MySQL 8.0 (Identity & Structured Data) |
| **NoSQL Document Database** | MongoDB 7.0 (Posts, Comments, Chat Messages) |
| **Graph Database** | Neo4j 5.15 (Social Connections & Follower Graphs) |
| **Caching & Locking** | Redis 7.2 (Spring Cache, Rate Limiting, Distributed Locks) |
| **Message Broker (EDA)** | Apache Kafka 3.8 (KRaft Mode - ZooKeeper-less) |
| **Inter-Service Calls** | gRPC (HTTP/2 + Protobuf), Spring Cloud OpenFeign (REST) |
| **Identity & Security** | Keycloak 24.0 (OIDC, OAuth2.0, JWT, PKCE), Spring Security 6 |
| **Fault Tolerance & Resilience** | Resilience4j (Circuit Breaker, RateLimiter, Retry, Bulkhead) |
| **Observability & Tracing** | Micrometer Tracing, OpenZipkin, Prometheus, Grafana, AKHQ |
| **Container & Orchestration** | Docker, Docker Compose v3.8 |

---

## 4. System Architecture

```text
                               +-----------------------------+
                               |     Client / Web / Mobile   |
                               +-----------------------------+
                                              |
                                      HTTP / REST / WS
                                              v
                               +-----------------------------+
                               |  Spring Cloud API Gateway   |
                               | (Reactive Redis Rate Limit) |
                               +-----------------------------+
                                              |
                        +---------------------+---------------------+
                        | Keycloak Auth Check | Eureka Discovery    |
                        +---------------------+---------------------+
                                              |
            +---------------------------------+---------------------------------+
            |                                 |                                 |
            v                                 v                                 v
   +-----------------+               +-----------------+               +-----------------+
   | Identity Service|               | Profile Service |               |   Post Service  |
   | (MySQL 8.0)     |               | (Neo4j Graph)   |               | (MongoDB 7.0)   |
   +-----------------+               +-----------------+               +-----------------+
            |                                 |                                 |
            |                         gRPC / REST Feign                         |
            +---------------------------------+---------------------------------+
                                              |
                                              v
                              +-------------------------------+
                              |    Apache Kafka Event Bus     |
                              |   (KRaft Mode Event Broker)   |
                              +-------------------------------+
                                     /                 \
                                    v                   v
                        +-------------------+   +-------------------+
                        |Notification Service|   |   Chat Service    |
                        | (Kafka Consumer)  |   | (WebSocket + Mongo|
                        +-------------------+   +-------------------+
```

---

## 5. Project Structure

```text
mini-blog/
├── .env                          # Local environment variables
├── .env.example                  # Template environment configuration
├── docker-compose.yml            # Multi-container infrastructure stack
├── docs/                         # In-depth architectural & learning guides
│   ├── 00-microservices-learning-plan.md
│   ├── 01-infrastructure-and-docker.md
│   ├── 02-service-discovery-eureka.md
│   ├── 03-centralized-config-server.md
│   ├── 04-keycloak-oauth2-gateway.md
│   ├── 05-resilience4j-fault-tolerance.md
│   ├── 06-grpc-vs-rest-feign.md
│   ├── 07-saga-pattern-and-outbox.md
│   ├── 08-redis-distributed-caching.md
│   └── 09-distributed-observability.md
├── api-gateway/                  # Edge Gateway with Keycloak OIDC & Rate Limiting
├── identity-service/             # Authentication & Keycloak integration
├── profile-service/              # User profile & Neo4j social graph
├── post-service/                 # Blog post CRUD, comments & reaction feed
├── notification-service/         # Async event notification consumer
└── chat-service/                 # Real-time WebSocket messaging service
```

---

## 6. Authentication & Authorization

* **Identity Provider**: Powered by Keycloak OIDC (OpenID Connect) Server on Realm `mini-blog-realm`.
* **OAuth2.0 Flows**:
  * **Authorization Code with PKCE** for frontend/web clients.
  * **Client Credentials** for secure service-to-service internal communication.
* **Token Validation**: The API Gateway intercepts all incoming requests, decodes and verifies RSA-signed JWT Access Tokens from Keycloak, and forwards user claims (Roles, User ID, Email) downstream via custom headers.
* **Rate Limiting**: Reactive Redis Rate Limiter configured at the API Gateway to prevent brute-force attacks and DDoS (Token Bucket algorithm).

---

## 7. API & Communication Protocols

### Inter-Service Communication Strategies
1. **gRPC (Protobuf over HTTP/2)**: Utilized for high-frequency, low-latency synchronous calls (e.g., `post-service` fetching author details from `profile-service`).
2. **Spring Cloud OpenFeign (REST)**: Utilized for flexible HTTP APIs wrapped with **Resilience4j Circuit Breakers**.
3. **Apache Kafka (Asynchronous Messaging)**: Used for non-blocking event publishing (e.g., `UserRegisteredEvent`, `PostPublishedEvent`, `NotificationEvent`).

---

## 8. Getting Started

### Prerequisites
* **Java Development Kit (JDK 21)** or higher.
* **Docker Desktop** & **Docker Compose**.
* **Maven 3.9+**.

### Step 1: Environment Setup
Copy the `.env.example` template to `.env`:
```bash
cp .env.example .env
```

### Step 2: Start Infrastructure Stack
Launch MySQL, MongoDB, Neo4j, Redis, Kafka, AKHQ, Keycloak, and Zipkin via Docker Compose:
```bash
docker-compose up -d
```

### Step 3: Verify Infrastructure Health
Check container status:
```bash
docker-compose ps
```

---

## 9. Docker & Infrastructure Deployment

The infrastructure services exposed by `docker-compose.yml`:

| Service | Container Port | Host Port | Management Web UI / Endpoint |
| :--- | :--- | :--- | :--- |
| **MySQL** | `3306` | `3307` | [http://localhost:3307](http://localhost:3307) |
| **MongoDB** | `27017` | `27017` | [http://localhost:27017](http://localhost:27017) |
| **Neo4j Graph** | `7474`, `7687` | `7474`, `7687` | [http://localhost:7474](http://localhost:7474) |
| **Redis** | `6379` | `6379` | [http://localhost:6379](http://localhost:6379) |
| **Kafka (KRaft)** | `9092`, `9094` | `9094` | [http://localhost:9094](http://localhost:9094) |
| **AKHQ (Kafka UI)**| `8080` | `8085` | [http://localhost:8085](http://localhost:8085) |
| **Keycloak** | `8080` | `8081` | [http://localhost:8081](http://localhost:8081) |
| **Zipkin Tracing** | `9411` | `9411` | [http://localhost:9411](http://localhost:9411) |

---

## 10. Technical Highlights & Design Patterns

* **Transactional Outbox Pattern**: Guarantees zero event loss during database writes and Kafka event publishing by persisting events into an Outbox table within the same local database transaction.
* **Saga Pattern (Choreography/Orchestration)**: Manages distributed transactions across `identity-service`, `profile-service`, and `notification-service` with automated compensating transactions on failure.
* **Cache-Aside & Write-Through Patterns**: Multi-tier caching with Spring Cache & Redis to eliminate redundant database queries for hot blog feeds.
* **Circuit Breaker & Fallback**: Short-circuits failing internal/external dependency calls using Resilience4j sliding window metrics and returns graceful fallbacks.
* **Distributed Observability**: End-to-end W3C/B3 trace context propagation across HTTP headers and Kafka metadata headers via Micrometer & OpenZipkin.

---

## 11. Detailed Architecture Documentation

For deep technical insights, underlying mechanisms, and architectural trade-offs, refer to the guides in the [`docs/`](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs) directory:

* [00. Master Strategy & Microservices Learning Plan](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/00-microservices-learning-plan.md)
* [01. Containerization & Docker Networking Setup](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/01-infrastructure-and-docker.md)
* [02. Service Discovery Mechanics (Spring Cloud Eureka)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/02-service-discovery-eureka.md)
* [03. Centralized Configuration Management (Spring Cloud Config)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/03-centralized-config-server.md)
* [04. Identity Provider & Gateway Security (Keycloak + Spring Cloud Gateway)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/04-keycloak-oauth2-gateway.md)
* [05. Fault Tolerance & Circuit Breaking (Resilience4j)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/05-resilience4j-fault-tolerance.md)
* [06. High-Performance Inter-Service Calls (gRPC vs REST Feign)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/06-grpc-vs-rest-feign.md)
* [07. Distributed Transactions (Saga Pattern & Outbox Pattern)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/07-saga-pattern-and-outbox.md)
* [08. Distributed Caching Strategies (Redis & Redisson)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/08-redis-distributed-caching.md)
* [09. Distributed Observability (Prometheus, Grafana & Zipkin)](file:///c:/workspace/Backend/personal-projects/microservices/mini-blog/docs/09-distributed-observability.md)

---

## 12. Future Improvements

* [ ] Implement Debezium CDC (Change Data Capture) for streaming database Outbox changes directly to Kafka.
* [ ] Integrate Elasticsearch for full-text search across blog posts, tags, and user profiles.
* [ ] Grafana Dashboards for microservice metrics monitoring (JVM, Kafka throughput, Redis hit/miss rates).
* [ ] Kubernetes (k8s) manifests & Helm charts for cloud-native deployment.
