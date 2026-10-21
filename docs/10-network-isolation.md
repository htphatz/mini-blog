# Phase 10: Service Isolation & Network Segmentation (Loopback Binding, Docker Networks, Kubernetes NetworkPolicy)

## 1. Architectural Motivation (Why Network Isolation?)

Every service in `mini-blog` is an independent OAuth2 **Resource Server** that validates the JWT on its own. The `api-gateway` is the intended single entry point, but a service port that is reachable from the outside makes the gateway optional.

### The Problem With Exposed Service Ports:
1. **Gateway Bypass**: Anyone holding a valid JWT can call `:8083` (`post-service`) directly. The gateway only routes and relays the token today, but it is the place where rate limiting, CORS, logging, and IP blocking will live. A side door defeats all of them.
2. **Unauthenticated Internal Endpoints**: `profile-service` exposes `/internal/**` as `permitAll` (K-1). If that port is reachable from outside, anyone can call it without a token.
3. **Docker Bypasses the Host Firewall**: Ports published as `'${PORT}:...'` bind `0.0.0.0`, and Docker adds its own iptables rules, so `ufw` does **not** protect them. On a VPS the databases would be exposed to the internet.

### The Solution: Only the Gateway Is Reachable From Outside
Services accept connections only from the gateway (and from each other) over an internal network. The rest of this document describes the current single-machine model (loopback binding) and the path to real internal networks.

> **Related:** [`04-keycloak-oauth2-gateway.md`](04-keycloak-oauth2-gateway.md), [`../KEYCLOAK.md`](../KEYCLOAK.md) (K-1, K-2, K-16), [`../KEYCLOAK-ENDPOINTS.md`](../KEYCLOAK-ENDPOINTS.md).

---

## 2. Under-the-Hood Mechanics

### A. Bind Address: Loopback vs All Interfaces

```
Outside client ──► api-gateway  0.0.0.0:8888 ──► post-service 127.0.0.1:8083
                                            ──► profile-service 127.0.0.1:8081 (+ gRPC 127.0.0.1:9091)
                                            ──► identity / notification / chat (127.0.0.1)
                   discovery-service (Eureka) 127.0.0.1:8761
```

A socket bound to `127.0.0.1` only accepts connections that originate on the same host. A socket bound to `0.0.0.0` accepts connections on every network interface. This model fits when **all services run on one machine** (local development, or a single VPS).

### B. Why `eureka.instance.ip-address` Must Match the Bind Address

All services set `prefer-ip-address: true`, so by default Eureka registers the LAN IP (for example `192.168.x.x`). If a service listens only on `127.0.0.1` while Eureka advertises the LAN IP, the gateway connects to the LAN IP and is refused. Registering `127.0.0.1` makes both sides agree. The gRPC port of `profile-service` is discovered through Eureka too (`discovery:///profile-service` + the `grpc-port` metadata), so the same rule applies.

### C. Docker Published Ports and iptables
Docker inserts its own NAT rules for published ports, evaluated before `ufw`. Publishing to `127.0.0.1:<port>:<port>` keeps the port reachable from the host only.

---

## 3. Implementation Blueprint in `mini-blog`

### A. Service Configuration (identity, profile, post, notification, chat, discovery)

```yaml
server:
  port: 8083
  address: 127.0.0.1            # listen on loopback only, other machines cannot connect

eureka:
  instance:
    prefer-ip-address: true
    ip-address: 127.0.0.1       # register loopback so the gateway can still route lb:// calls
```

`profile-service` also binds its gRPC server:

```yaml
grpc:
  server:
    port: 9091
    address: 127.0.0.1
```

`api-gateway` is **unchanged** (listens on all interfaces).

### B. Infrastructure Ports in `docker-compose.yml`

```yaml
ports:
  - '127.0.0.1:${MYSQL_PORT}:3306'
```

Applied to MySQL, MongoDB, Neo4j (7474, 7687), Redis, Kafka, AKHQ, and Zipkin. **Keycloak is not bound to loopback yet** because the browser must reach the login page; on a VPS, put nginx in front and then bind Keycloak to `127.0.0.1`.

Apply with `docker compose up -d` (containers are recreated, data in volumes is kept).

### C. Verification

Restart in this order: `discovery-service` → the other services → `api-gateway`.

```powershell
netstat -ano | findstr LISTENING | findstr "8080 8081 8082 8083 8084 8888 8761 9091"
```

Expected: only `8888` is `0.0.0.0`/`[::]`; everything else is `127.0.0.1`.

```powershell
# Eureka must register loopback addresses
curl http://localhost:8761/eureka/apps -H "Accept: application/json" | findstr ipAddr
```

Expected: every `ipAddr` is `127.0.0.1`. Then call one API through the gateway (`/api/v1/post/...` with a token) to confirm routing still works.

To prove a service is really blocked, call it through the LAN IP or from another machine:

| Caller | `:8888` (gateway) | `:8083` (post-service) |
| :--- | :--- | :--- |
| Another machine (LAN, internet if VPS) | Allowed | Connection refused |
| This machine (`localhost`) | Allowed | **Still allowed** (loopback belongs to this machine) |
| Gateway, same machine | | Allowed, via Eureka to `127.0.0.1:8083` |

### D. Limitations
- **Blocks other machines only.** Another process on the same host, or someone who has SSH access to the VPS, can still call services directly.
- **Does not work across machines.** Calls between hosts fail because Eureka advertises `127.0.0.1`.
- **Not a replacement for application-layer security.** These gaps remain: K-1 (`/internal/**` is `permitAll`), K-2 (no `aud` check), K-16 (notification/chat use `anyRequest().permitAll()`), K-5/K-17 (no ownership checks).

---

## 4. Scaling the Isolation Model Beyond One Machine

Principle: **the application listens on `0.0.0.0`; the network layer decides who may connect.** Do not hardcode per-environment IPs in `application.yml`.

### A. Parameterize the Configuration

```yaml
server:
  address: ${SERVER_ADDRESS:127.0.0.1}      # dev: loopback; container/prod: 0.0.0.0

spring:
  cloud:
    inetutils:
      preferred-networks:
        - ${PRIVATE_NETWORK_REGEX:^127\.}   # register an IP from the private range with Eureka

eureka:
  instance:
    prefer-ip-address: true                 # drop the hardcoded ip-address

management:
  server:
    port: ${MANAGEMENT_PORT:9000}           # separate actuator port, not routed through the gateway
```

### B. Docker Compose: Publish Only the Gateway

```yaml
services:
  api-gateway:
    ports: ["8888:8888"]        # the ONLY service that publishes a port
    networks: [edge, backend]

  post-service:
    environment:
      SERVER_ADDRESS: 0.0.0.0
    expose: ["8083"]            # documentation only
    networks: [backend]         # no ports: unreachable from the host or outside

networks:
  edge: {}
  backend: {}
```

Note: `internal: true` also blocks outbound internet traffic. `notification-service` calls Brevo (`api.brevo.com`), so it needs an additional network with outbound access.

### C. Kubernetes

```yaml
apiVersion: v1
kind: Service
metadata: { name: post-service }
spec:
  type: ClusterIP
  selector: { app: post-service }
  ports: [{ port: 8083 }]
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata: { name: post-service-allow-gateway }
spec:
  podSelector: { matchLabels: { app: post-service } }
  policyTypes: [Ingress]
  ingress:
    - from:
        - podSelector: { matchLabels: { app: api-gateway } }
      ports: [{ port: 8083 }]
```

### D. Multiple VPS: Private Network + Firewall Allow-List

```bash
# post-service at 10.0.0.12, gateway at 10.0.0.5
ufw default deny incoming
ufw allow from 10.0.0.5 to any port 8083 proto tcp   # gateway only
ufw allow 443/tcp                                     # public nginx/gateway only
```

On a cloud provider: a Security Group that only accepts traffic from the gateway's Security Group, and a private subnet with no route to the internet.

---

## 5. Trade-Off Analysis: Loopback Binding vs Docker Networks vs Kubernetes NetworkPolicy

| Stage | Mechanism | Strength | Weakness |
| :--- | :--- | :--- | :--- |
| Dev on one machine (current) | Bind `127.0.0.1` + Eureka `127.0.0.1` | Zero infrastructure, simple to reason about | Single host only; same-host processes still get in |
| One VPS with Docker | Compose private networks, only the gateway publishes a port, DBs bound to `127.0.0.1` | Isolation enforced by the container network | Needs a Compose restructure; a second network is required for outbound calls |
| Multiple machines / production | K8s `ClusterIP` + `NetworkPolicy` (or Security Group + private subnet) + `aud` check + service token (+ mTLS) | Real zero-trust segmentation | Operational complexity, requires a cluster or cloud networking |

---

## 6. Remaining Work (Application Layer)

| Code | Task | Note |
| :--- | :--- | :--- |
| K-1 | Service-to-service token (`client_credentials`); remove `permitAll` from `/internal/**` | Network isolation only protects from the outside |
| K-2 | Check `aud` (Audience mapper in Keycloak + `audiences`) | `iss` is already checked through `issuer-uri` |
| K-16 | notification/chat: remove `anyRequest().permitAll()` | |
| K-11 | Gateway removes client-supplied `X-User-Id`/`X-Username` | No service reads them today |

---
*Stored at `docs/10-network-isolation.md` for reference.*
