# Phase 3: Centralized Configuration Management (Spring Cloud Config Server)

## 1. Architectural Motivation (Why Centralized Configuration?)

In a distributed microservices environment with dozens or hundreds of services, embedding configuration files (`application.yml` or `application.properties`) directly inside each application jar binary creates critical operational challenges:

### Problems With Embedded Configurations:
1. **Configuration Drift**: Inconsistent database credentials, thread pool limits, or feature flags across different deployment environments (Dev, Staging, Prod).
2. **Security & Compliance Risk**: Storing sensitive DB passwords, JWT secret keys, and API tokens directly in source code repositories.
3. **Deployment Friction (Restart Requirement)**: Modifying a simple log level or feature flag requires rebuilding, re-testing, and re-deploying the entire service container.
4. **Lack of Auditability**: Difficulty tracking *who* changed a configuration setting, *when*, and *why*.

### The Solution: Externalized Centralized Config Server
Centralized Configuration separates code from configuration. A dedicated Config Server fetches configuration parameters from a central backing repository (e.g., Git, HashiCorp Vault, or Local File System) and serves them securely to microservices at boot time and runtime.

---

## 2. Under-the-Hood Mechanics

### A. Spring Cloud Config Architecture

```
                                +-----------------------------------+
                                | Git Repository / Config Storage   |
                                | (identity-service-dev.yml, etc.)  |
                                +-----------------------------------+
                                                  ^
                                                  | Fetch configs
                                                  v
                                +-----------------------------------+
                                |    Config Server (config-service) |
                                |              Port 8888            |
                                +-----------------------------------+
                                     ^            ^            ^
           1. Fetch Config at Boot   |            |            |
      +------------------------------+            |            +-----------------------------+
      |                                           |                                          |
+------------------+                    +------------------+                        +------------------+
| identity-service |                    |   post-service   |                        |  profile-service |
+------------------+                    +------------------+                        +------------------+
```

1. **Bootstrap Phase**: During startup, a client microservice contacts `config-service` before initializing its Spring `ApplicationContext`.
2. **Property Source Environment Composition**: The Config Server locates files matching `{application}-{profile}.yml` (e.g., `identity-service-dev.yml`) and returns a hierarchical `PropertySource`.

### B. Dynamic Hot-Reloading Mechanics (`@RefreshScope` & Spring Cloud Bus)

Updating configuration at runtime without restarting application containers:

1. **`@RefreshScope` Annotation**:
   - Beans annotated with `@RefreshScope` are wrapped by a Spring CGLIB proxy.
   - When a refresh event occurs, the target bean instance is destroyed and evicted from memory. Upon the next method call, Spring lazily re-instantiates the bean with updated property values.

2. **Spring Cloud Bus with Apache Kafka (Cluster-wide Refresh)**:
   - Calling `/actuator/refresh` on individual microservices does not scale.
   - **Spring Cloud Bus** connects all microservices via an event topic (Kafka `springCloudBus`).
   - Sending a single POST request to `http://config-service/actuator/busrefresh` publishes a `RefreshRemoteApplicationEvent` to Kafka. All connected microservices consume the event and refresh their `@RefreshScope` beans simultaneously.

```
POST /actuator/busrefresh 
  ---> Config Server ---> [Kafka Topic: springCloudBus] ---> Service A (Refreshed)
                                                        ---> Service B (Refreshed)
                                                        ---> Service C (Refreshed)
```

---

## 3. Property Encryption & Security

To avoid storing plaintext passwords in Git:
1. Config Server configures a symmetric or asymmetric RSA keypair.
2. Encrypted properties are stored in Git using the `{cipher}` prefix:
   ```yaml
   spring:
     datasource:
       password: '{cipher}AQA6Bf0x9K...encrypted_hash...'
   ```
3. Config Server automatically decrypts the secret before delivering the payload over HTTPS to authenticated client microservices.

---

## 4. Implementation Blueprint in `mini-blog`

### A. Config Server Module (`config-service`)
- **Port**: `8888`
- **Dependencies**: `spring-cloud-config-server`, `spring-cloud-starter-bus-amqp`/`kafka`
- **Main Annotation**: `@EnableConfigServer`

### B. Client Configuration (`identity-service`, `post-service`, etc.)
- Add `spring-cloud-starter-config` dependency.
- Configure `application.yml` (or `bootstrap.yml`):
  ```yaml
  spring:
    application:
      name: identity-service
    config:
      import: "configserver:http://localhost:8888"
    profiles:
      active: dev
  ```

---

## 5. Trade-Off Analysis: Spring Cloud Config vs Kubernetes ConfigMap/Secret vs HashiCorp Vault

| Dimension | Spring Cloud Config | Kubernetes ConfigMap / Secret | HashiCorp Vault |
| :--- | :--- | :--- | :--- |
| **Primary Focus** | Native Spring Boot integration, Git-backed versioning | Kubernetes-native container environment injection | High-security secrets management, dynamic leasing |
| **Hot-Reloading** | `@RefreshScope` + Spring Cloud Bus | File mount polling / pod rollout restart | Vault Agent / Spring Vault reactive listeners |
| **Secret Encryption** | Cipher text in Git via Config Server RSA key | Base64 (not encryption!) unless KMS enabled | Enterprise-grade encryption at rest (AES-256) |
| **Framework Agnostic** | Java/Spring centric | Native to any language running in K8s | Language-agnostic REST API & SDKs |

---
*Stored at `docs/03-centralized-config-server.md` for reference.*
