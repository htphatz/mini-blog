# Phase 4: Enterprise Identity Provider & API Gateway Security (Keycloak + Spring Cloud Gateway)

## 1. Architectural Motivation (Why Keycloak & Gateway Security?)

In a monolithic application, user authentication is typically handled via server-side HTTP Sessions or custom JWT utilities embedded directly in the application layer. In a **Microservices Architecture**, managing authentication independently inside every service leads to severe architectural flaws:

### Problems With Distributed/Custom Authentication:
1. **Security Vulnerability & Duplicated Code**: Every microservice must re-implement password hashing, token generation, token validation, and refresh token rotation logic.
2. **Lack of Single Sign-On (SSO)**: Users must log in separately across different client applications.
3. **Identity Fragmentation**: User data is scattered across databases rather than stored in a centralized, secure Identity Provider (IdP).
4. **Gateway Bypass & Unprotected Edge**: Without a centralized security barrier at the entry point (API Gateway), internal services are exposed to unauthorized external traffic.

### The Solution: Keycloak IAM + Centralized Gateway Token Validation
- **Keycloak**: An open-source enterprise Identity and Access Management (IAM) server providing out-of-the-box Single Sign-On (SSO), Social Login, Multi-Factor Authentication (MFA), User Directories (LDAP/AD), and OAuth2.0 / OpenID Connect (OIDC) compliance.
- **API Gateway Security Barrier**: `api-gateway` validates incoming OAuth2 JWT tokens at the edge network boundary before routing clean, authorized requests downstream.

---

## 2. Under-the-Hood Mechanics

### A. OAuth 2.0 vs OpenID Connect (OIDC)

| Protocol | Primary Purpose | Key Artifact | Key Endpoint / Flow |
| :--- | :--- | :--- | :--- |
| **OAuth 2.0** | **Authorization** (Delegated Access to Resources) | `Access Token` (JWT or Opaque) | `/oauth/token` |
| **OpenID Connect (OIDC)** | **Authentication** (Verifying *Who* the user is) | `ID Token` + `UserInfo` payload | `/openid-connect/auth`, `/.well-known/openid-configuration` |

### B. OIDC Authorization Code Flow with PKCE (Proof Key for Code Exchange)

Recommended flow for Single Page Applications (React/Vue) and Mobile Clients:

```
[User Browser]           [API Gateway]            [Keycloak IdP]
      |                        |                         |
      | 1. Login Request       |                         |
      |----------------------->|                         |
      | 2. Redirect to Keycloak Login Page               |
      |<-------------------------------------------------|
      | 3. Submit Credentials (Username/Password + PKCE) |
      |------------------------------------------------->|
      | 4. Returns Authorization Code                    |
      |<-------------------------------------------------|
      | 5. Exchange Code + PKCE Verifier for JWT Tokens  |
      |------------------------------------------------->|
      | 6. Returns Access Token (JWT) & Refresh Token    |
      |<-------------------------------------------------|
      | 7. API Request with `Authorization: Bearer <JWT>`|
      |----------------------->|                         |
      |                        | 8. Validate JWT via JWKS|
      |                        |------------------------>|
      |                        | 9. Forward Authorized   |
      |                        |    Request Downstream   |
```

### C. JWT Structure & JWKS Signature Verification

A JSON Web Token (JWT) consists of three Base64URL-encoded parts: `Header.Payload.Signature`.

1. **Decoupled Verification via JWKS (JSON Web Key Set)**:
   - Instead of sharing a static symmetric secret key across all services (security risk!), Keycloak signs tokens using an asymmetric private key (RSA-256).
   - API Gateway fetches Keycloak's public keys from the JWKS endpoint:
     `http://keycloak:8080/realms/mini-blog-realm/protocol/openid-connect/certs`
   - Gateway verifies incoming JWT signatures in-memory using cached public keys with zero network overhead per request.

2. **Claims Introspection**:
   - `sub`: Unique Keycloak User ID.
   - `preferred_username`: User's username.
   - `realm_access.roles`: User's assigned roles (`ROLE_USER`, `ROLE_ADMIN`).

### D. Reactive Redis Token Bucket Rate Limiting (API Gateway Filter)

To protect downstream microservices from Denial-of-Service (DoS) attacks and traffic spikes, API Gateway implements a **Redis-backed Token Bucket Algorithm**:

```
           +-------------------------------------+
           |          Token Bucket               |
           | Capacity = 20 tokens                |
           | Replenish Rate = 10 tokens/sec      |
           +-------------------------------------+
                             |
             Request Arrives | Token Available?
             --------------->+---> Yes: Consume 1 token & Allow Request
                             |
                             +---> No: Reject with HTTP 429 Too Many Requests
```

- **Algorithm Parameters**:
  - `replenishRate`: How many tokens are added to the bucket per second.
  - `burstCapacity`: The maximum number of tokens the bucket can hold (allows temporary bursts).
  - `requestedTokens`: Tokens consumed per request (typically 1).
- **Redis Integration**: Lua scripts execute atomically inside Redis to evaluate token counts for each IP address or User ID across clustered gateway instances.

---

## 3. Keycloak Setup & Configuration Blueprint

### A. Realm & Client Setup
- **Realm Name**: `mini-blog-realm`
- **Clients**:
  - `mini-blog-gateway`: Confidential/Public client for API Gateway & Frontend apps.
  - `identity-service-client`: Client Credentials flow for service-to-service communication.
- **Roles**: `ROLE_USER`, `ROLE_ADMIN`.

### B. Spring Cloud Gateway Security Configuration
Add `spring-boot-starter-oauth2-resource-server` and `spring-boot-starter-data-redis-reactive` to `api-gateway`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8484/realms/mini-blog-realm
          jwk-set-uri: http://localhost:8484/realms/mini-blog-realm/protocol/openid-connect/certs
  cloud:
    gateway:
      routes:
        - id: post-service
          uri: lb://post-service
          predicates:
            - Path=/api/v1/post/**
          filters:
            - StripPrefix=2
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@userKeyResolver}"
```

---

## 4. Trade-Off Analysis: Custom Security vs Keycloak IAM

| Dimension | Custom JWT in Identity Service | Keycloak Enterprise IAM |
| :--- | :--- | :--- |
| **Development Effort** | High (Must write token generation, refresh, rotation, revocation) | Low (Out-of-the-box OIDC/OAuth2 endpoints) |
| **Security Compliance** | Vulnerable to implementation flaws (weak signing, lack of rotation) | Enterprise-tested, compliant with FAPI, OIDC, SAML 2.0 |
| **User Directory Support** | Limited to custom DB schema | Native LDAP, Active Directory, Social Identity Providers |
| **SSO & MFA** | Extremely complex to build manually | Built-in TOTP, WebAuthn, Magic Links, Passkeys |

---
*Stored at `docs/04-keycloak-oauth2-gateway.md` for reference.*
