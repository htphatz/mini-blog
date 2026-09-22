package com.htphatz.identity_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.htphatz.event.NotificationEvent;
import com.htphatz.identity_service.dto.request.LoginRequest;
import com.htphatz.identity_service.dto.request.LogoutRequest;
import com.htphatz.identity_service.dto.request.ProfileRequest;
import com.htphatz.identity_service.dto.request.RegisterRequest;
import com.htphatz.identity_service.dto.response.LoginResponse;
import com.htphatz.identity_service.dto.response.UserResponse;
import com.htphatz.identity_service.entity.OutboxEvent;
import com.htphatz.identity_service.entity.User;
import com.htphatz.identity_service.enums.EventType;
import com.htphatz.identity_service.enums.UserStatus;
import com.htphatz.identity_service.exception.AppException;
import com.htphatz.identity_service.exception.ErrorCode;
import com.htphatz.identity_service.mapper.ProfileMapper;
import com.htphatz.identity_service.repository.OutboxEventRepository;
import com.htphatz.identity_service.repository.UserRepository;
import com.htphatz.identity_service.repository.httpclient.ProfileClient;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final ProfileClient profileClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProfileMapper profileMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.server-url:http://localhost:8484}")
    private String serverUrl;

    @Value("${keycloak.realm:mini-blog-realm}")
    private String realm;

    @Value("${keycloak.client-id:identity-service-client}")
    private String clientId;

    @Value("${keycloak.client-secret:4zt3bb1sT6GhIxCG20Gw4PrvaHD7S1DO}")
    private String clientSecret;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder().baseUrl(serverUrl).build();
    }

    public UserResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // 1. Prepare UserRepresentation for Keycloak
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername(request.getEmail());
        userRepresentation.setEmail(request.getEmail());
        userRepresentation.setFirstName(request.getFirstName());
        userRepresentation.setLastName(request.getLastName());
        userRepresentation.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credential));

        // 2. Create User in Keycloak via Admin Client
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(userRepresentation);

        if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
            log.warn("User already exists in Keycloak: {}", request.getEmail());
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            log.error("Failed to create user in Keycloak, HTTP status: {}", response.getStatus());
            throw new AppException(ErrorCode.USER_CREATION_FAILED);
        }

        // 3. Extract generated Keycloak user UUID
        String userId = CreatedResponseUtil.getCreatedId(response);
        log.info("User created in Keycloak with ID: {}", userId);

        // 4. Assign default USER role in Keycloak
        try {
            RoleRepresentation userRole = null;
            try {
                userRole = keycloak.realm(realm).roles().get("ROLE_USER").toRepresentation();
            } catch (Exception ex) {
                userRole = keycloak.realm(realm).roles().get("USER").toRepresentation();
            }
            if (userRole != null) {
                usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(userRole));
                log.info("Assigned role {} in Keycloak to user ID: {}", userRole.getName(), userId);
            }
        } catch (Exception e) {
            log.warn("Could not assign default role in Keycloak: {}", e.getMessage());
        }

        // 5. Save user record locally in MySQL with initial status PENDING (Saga step 1)
        User localUser = User.builder()
                .id(userId)
                .email(request.getEmail())
                .status(UserStatus.PENDING)
                .build();
        try {
            userRepository.save(localUser);
            log.info("Saved local user record with status PENDING for user ID: {}", userId);
        } catch (DataIntegrityViolationException exception) {
            log.warn("User record already exists in local DB: {}", exception.getMessage());
        }

        // 6. Saga Forward Step: Create Profile in profile-service (Neo4j)
        // Must succeed BEFORE saving Outbox to completely prevent premature email dispatch (Race Condition)
        try {
            ProfileRequest profileRequest = profileMapper.toProfileRequest(request);
            profileRequest.setUserId(userId);
            profileClient.createProfile(profileRequest);
            log.info("Saga Forward Step: Profile created successfully for user ID: {}", userId);

            // Forward step succeeded: promote status to ACTIVE
            localUser.setStatus(UserStatus.ACTIVE);
            userRepository.save(localUser);

            // 7. Transactional Outbox Pattern: Save event to MySQL now that Profile is confirmed!
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipient(request.getEmail())
                    .params(Map.of("name", request.getFirstName() + " " + request.getLastName()))
                    .build();

            String payloadJson = "{}";
            try {
                payloadJson = objectMapper.writeValueAsString(notificationEvent);
            } catch (Exception e) {
                log.error("Failed to serialize NotificationEvent to JSON", e);
            }

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("USER")
                    .aggregateId(userId)
                    .eventType(EventType.WELCOME_BLOG)
                    .payload(payloadJson)
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Saved Outbox event id: {} for user: {}", outboxEvent.getId(), userId);

        } catch (Exception ex) {
            log.error("Saga Forward Step FAILED for user ID: {}. Triggering Compensating Transactions...", userId, ex);
            executeRegistrationCompensation(userId, localUser);
            throw new AppException(ErrorCode.USER_CREATION_FAILED);
        }

        // 8. Return response
        return UserResponse.builder()
                .id(userId)
                .email(request.getEmail())
                .roles(Set.of("USER"))
                .build();
    }

    /**
     * Saga Compensating Transaction: Rollbacks distributed operations in reverse order.
     * Note: Outbox event does not need deletion because it is only saved after Profile succeeds!
     */
    private void executeRegistrationCompensation(String userId, User localUser) {
        log.warn("Executing Saga Compensation for User ID: {}", userId);

        // Compensation Step 1: Rollback Keycloak user
        try {
            keycloak.realm(realm).users().get(userId).remove();
            log.info("[Saga Compensation] Successfully deleted user {} from Keycloak", userId);
        } catch (Exception e) {
            log.error("[Saga Compensation] Failed to delete user {} from Keycloak", userId, e);
        }

        // Compensation Step 2: Rollback MySQL User State
        try {
            localUser.setStatus(UserStatus.FAILED);
            userRepository.save(localUser);
            log.info("[Saga Compensation] Updated user {} status to FAILED in MySQL", userId);
        } catch (Exception e) {
            log.error("[Saga Compensation] Failed to update user status in MySQL", e);
        }
    }

    /**
     * [DEV/TESTING ONLY - ANTI-PATTERN IN PROD]
     * Uses 'grant_type=password' (ROPC), which is deprecated in OAuth 2.1 because the backend
     * directly handles raw credentials and bypasses Keycloak MFA/SSO features.
     * Kept temporarily for developer convenience (Postman/cURL token generation).
     * Production login is handled at 'api-gateway' via OIDC Authorization Code Flow with PKCE
     * (see com.htphatz.api_gateway.config.SecurityConfig).
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Authenticating user via Keycloak: {}", request.getEmail());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "password");
        formData.add("username", request.getEmail());
        formData.add("password", request.getPassword());
        formData.add("scope", "openid profile email");

        try {
            LoginResponse response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(LoginResponse.class);

            log.info("User {} authenticated successfully via Keycloak", request.getEmail());
            return response;
        } catch (HttpClientErrorException ex) {
            log.warn("Authentication failed for user {}: {}", request.getEmail(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new AppException(ErrorCode.PASSWORD_INVALID);
            }
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        } catch (Exception ex) {
            log.error("Error during authentication for user {}: {}", request.getEmail(), ex.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public void logout(LogoutRequest request) {
        log.info("Logging out session in Keycloak");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", request.getRefreshToken());

        try {
            restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Session successfully logged out in Keycloak");
        } catch (HttpClientErrorException ex) {
            log.warn("Logout request failed in Keycloak: {}", ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Error during Keycloak logout: {}", ex.getMessage());
        }
    }
}
