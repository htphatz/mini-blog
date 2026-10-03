# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project nature

A simple, personal learning project: a mini-blog built as microservices while learning and applying each concept in practice (service discovery, config server, Keycloak/OAuth2, Resilience4j, gRPC vs Feign, Saga + Outbox, Redis caching, observability — see `docs/00`–`09`). It is not production-grade: favor straightforward, readable implementations that demonstrate a pattern over heavy abstraction. Some code intentionally keeps alternatives side by side (e.g. `PostService` keeps the commented-out Feign call next to the gRPC call) for comparison — don't "clean up" these without asking. Commits follow `feat: ...` style, one learning topic per commit. Docs and code comments are a mix of Vietnamese and English.

## Tech stack

Java 21, Spring Boot 3.4.1–3.4.3, Spring Cloud 2024.0.0 (Gateway, Eureka, Config, OpenFeign), Spring Security 6 (OAuth2 resource server, JWT), Keycloak 24 (+ `keycloak-admin-client`), Spring Data JPA/MySQL, Spring Data MongoDB, Spring Data Neo4j, Spring Kafka (KRaft), gRPC + Protobuf (post/profile), Resilience4j (post-service), MapStruct + Lombok, springdoc-openapi (identity-service), WebSocket (chat-service), Brevo (email API).

**Planned, not yet in code:** Redis caching/rate limiting, Micrometer tracing + Zipkin, Debezium CDC. Redis and Zipkin containers exist in `docker-compose.yml` and `docs/08`–`09` describe them, but no service depends on them yet.

## Repository layout

Multi-module-in-spirit, but **no parent POM**: each top-level directory (`api-gateway`, `config-service`, `discovery-service`, `identity-service`, `profile-service`, `post-service`, `notification-service`, `chat-service`) is an independent Spring Boot 3 / Java 21 Maven project with its own `mvnw`. Run Maven from inside the service directory. `docs/` holds in-depth design guides (Saga/Outbox, gRPC vs Feign, Keycloak gateway, Resilience4j, etc.); `README.md` has the overview.

## Commands

Infrastructure (MySQL, MongoDB, Neo4j, Redis, Kafka KRaft, AKHQ, Keycloak, Zipkin) runs via Docker; services run on the host.

```bash
cp .env.example .env          # first time; fill in values
docker-compose up -d          # infra only (host ports: MySQL 3307, Mongo 27017, Neo4j 7474/7687, Redis 6379, Kafka 9094, AKHQ 8085, Keycloak 8484 via KEYCLOAK_PORT, Zipkin 9411)

cd <service> && ./mvnw spring-boot:run
cd <service> && ./mvnw test
cd <service> && ./mvnw test -Dtest=ClassName#method     # single test
cd <service> && ./mvnw clean package
```

Start order: `discovery-service` (8761) → other services → `api-gateway` (`config-service` is optional and shares port 8888 with the gateway, so skip it unless you move one of them). Tests are only the default `*ApplicationTests` context-load stubs, which need the infra running.

## Architecture

Requests enter via `api-gateway` (Spring Cloud Gateway, WebFlux), which routes `/api/v1/{identity|profile|post|notification|chat}/**` to `lb://<service>` via Eureka with `StripPrefix=2`, relays the Keycloak token (`TokenRelay`), and validates JWTs against realm `mini-blog-realm`.

Ports: api-gateway 8888, identity 8080, profile 8081 (+ gRPC 9091), notification 8082, post 8083, chat 8084.

Config: `config-service` is a Config Server in `native` mode serving `src/main/resources/config-repo/` (currently `application.yml` and `identity-service-dev.yml`). Only identity-service imports it (`optional:configserver:`); other services use their local `application.yml`.

Persistence is polyglot: identity → MySQL; profile → Neo4j (social graph); post/notification/chat → MongoDB; Redis is provisioned but not used yet.

## Claude Code setup

- `.claude/settings.json` (shared): allowed/ask/denied commands, secret-file read blocks, and hooks. A `PostToolUse` hook compiles the affected service after any `.java` edit (offline: `mvnw -o`) and reports errors; a `PreToolUse` hook blocks edits to `.env` (`.env.example` is fine). Personal overrides go in `.claude/settings.local.json` / `CLAUDE.local.md` (git-ignored).
- Subagents in `.claude/agents/`: `microservice-reviewer` (read-only convention/security review of the diff) and `pattern-explainer` (explains a pattern using this project's code and `docs/`).

## Rules loaded on demand

Detailed guidance lives in `.claude/rules/` and loads only when working on matching files:

- `code-style.md` — package layout and coding conventions for service Java code.
- `cross-service-contracts.md` — registration saga + outbox, gRPC `profile.proto`, and `NotificationEvent` (duplicated across services; keep copies in sync).

## Gotchas

- Secrets are `${ENV_VAR}` placeholders with no default, named exactly like the variables in `.env` (`MYSQL_ROOT_PASSWORD`, `MONGO_ROOT_USERNAME`/`MONGO_ROOT_PASSWORD`, `NEO4J_USER`/`NEO4J_PASSWORD`, `KEYCLOAK_CLIENT_SECRET`). Each service loads the repo-root `.env` via `spring.config.import: optional:file:./.env[.properties],optional:file:../.env[.properties]`, so run services from their own directory (or the repo root). `.env` must stay plain `KEY=value` lines (no inline comments, no `export`). Never reintroduce hardcoded defaults.
- Keycloak is expected at `localhost:8484` everywhere in service config, so set `KEYCLOAK_PORT=8484` in `.env`. `api-gateway` and `config-service` both default to port 8888, so run only one of them at a time (config-service is not in use yet); change one port, and the `spring.config.import` URL in `identity-service`, if you need both.
- The old Keycloak client secret was committed in git history; rotate it in Keycloak.
- `.env` holds local secrets; edit `.env.example` for new variables.
