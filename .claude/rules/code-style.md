---
paths:
  - "**/src/main/java/**/*.java"
---

# Code style

Every REST service uses the same layered package layout under `com.htphatz.<service_name>` (underscored): `config`, `controller`, `dto/request`, `dto/response`, `entity` (or `document` for Mongo), `enums`, `exception`, `mapper`, `repository` (+ `repository/httpclient` for Feign clients and fallbacks), `service` (+ `service/grpc`). Mirror this when adding a feature.

- **Injection/boilerplate**: Lombok everywhere — `@RequiredArgsConstructor` with `private final` fields (no `@Autowired`), `@Slf4j` for logging, `@Builder`/`@Getter` on DTOs.
- **Controllers**: thin; no business logic. Annotated `@RestController` with method-level mappings only (no class-level `@RequestMapping` — the gateway's `StripPrefix=2` supplies the prefix), `@Valid` on request bodies, and always return `APIResponse<T>` built via `APIResponse.<T>builder().result(...).build()`. Lists use `PageDto<T>` with 1-based `pageNumber` / `pageSize` request params.
- **Services**: concrete classes (no interface + impl), get the current user via `SecurityContextHolder.getContext().getAuthentication().getName()` (the Keycloak user id), and map with MapStruct.
- **Mapping**: MapStruct interfaces with `@Mapper(componentModel = "spring")`; entities are never returned directly.
- **Errors**: throw `new AppException(ErrorCode.X)`. Each service has its own `ErrorCode` enum `(Integer code, String message, HttpStatus)` and a `@RestControllerAdvice` `GlobalExceptionHandler` that converts to `APIResponse`. Add new codes to the enum rather than ad-hoc exceptions.
- **Security**: each service is its own JWT resource server (`WebSecurityConfig`, `JwtDecoderConfig`, `KeycloakRoleConverter` mapping Keycloak realm roles to authorities); this is duplicated per service rather than shared.
- **Resilience**: Feign clients get a `FallbackFactory` (`*ClientFallbackFactory`) returning degraded-but-valid data, with Resilience4j settings in `Resilience4jConfig`/yml. Currently only post-service uses Resilience4j.
- **Events**: services never publish to Kafka directly from business logic when consistency matters — write an `OutboxEvent` and let the scheduled publisher send it (at-least-once).
- **Comments**: numbered step comments and short Javadoc explaining the pattern being demonstrated (e.g. "Saga Forward Step", "Polling Publisher") are the norm — keep that explanatory tone for new patterns.
