# Keycloak providers

JAR files in this folder are mounted into the Keycloak container at `/opt/keycloak/providers` (see `docker-compose.yml`). `start-dev` rebuilds Keycloak with them on startup. `*.jar` is git-ignored, so download the plugin yourself.

## keycloak-kafka (event listener → Kafka)

Publishes Keycloak user events to Kafka. Used so a user who registers on the Keycloak page triggers profile creation + welcome email (see `docs/13-keycloak-kafka-register-events.md`).

- Source: https://github.com/SnuK87/keycloak-kafka
- Version: `1.3.0`, file `keycloak-kafka-1.3.0-jar-with-dependencies.jar`
- Download: https://github.com/SnuK87/keycloak-kafka/releases/download/1.3.0/keycloak-kafka-1.3.0-jar-with-dependencies.jar
- SHA-256: `57b55af7a392b44acbf68f4dae72b3b2fb3cd7dc0f1818b4c9cd107355580cfd`
- **Do not use 1.4.0**: it is compiled for Java 21 (class file 65) but Keycloak 24.0 runs on Java 17 (61), so Keycloak fails to start (`UnsupportedClassVersionError`). 1.3.0 is Java 17 and starts fine on Keycloak 24.0.5 (log: `Init kafka module ...`).
- The project README only lists Keycloak 19/21, so re-check `docker logs keycloak` after any Keycloak or plugin upgrade.

```bash
curl -L -o keycloak/providers/keycloak-kafka-1.3.0-jar-with-dependencies.jar \
  https://github.com/SnuK87/keycloak-kafka/releases/download/1.3.0/keycloak-kafka-1.3.0-jar-with-dependencies.jar
sha256sum keycloak/providers/keycloak-kafka-1.3.0-jar-with-dependencies.jar
docker-compose up -d keycloak   # recreate so the new volume/env are applied
```

Configuration is passed as environment variables on the `keycloak` service in `docker-compose.yml` (`KAFKA_TOPIC`, `KAFKA_CLIENT_ID`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_EVENTS`). The plugin must be configured, otherwise Keycloak fails to start.

After starting: Admin Console → realm `mini-blog-realm` → Realm settings → Events → Event listeners → add `kafka`.
