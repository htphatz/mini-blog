package com.htphatz.profile_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.htphatz.event.NotificationEvent;
import com.htphatz.profile_service.dto.event.KeycloakEvent;
import com.htphatz.profile_service.dto.request.ProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakEventConsumer {
    private static final String WELCOME_TOPIC = "welcome-blog";

    private final ObjectMapper objectMapper;
    private final ProfileService profileService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "keycloak-events", groupId = "profile-group")
    public void handleKeycloakEvent(String message) {
        KeycloakEvent event;
        try {
            event = objectMapper.readValue(message, KeycloakEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Cannot parse Keycloak event, skipping: {}", message, e);
            return;
        }

        if (!"REGISTER".equals(event.getType()) || !StringUtils.hasText(event.getUserId())) {
            log.debug("Ignoring Keycloak event of type {}", event.getType());
            return;
        }

        String userId = event.getUserId();
        Map<String, String> details = event.getDetails() != null ? event.getDetails() : Map.of();
        String email = details.get("email");
        String firstName = firstNonBlank(details.get("first_name"), details.get("username"), localPart(email));
        String lastName = details.get("last_name");
        log.info("Keycloak REGISTER event for user ID: {}", userId);

        // Saga Forward Step: create Profile (idempotent). Failure throws -> Kafka retries the record.
        profileService.createProfile(ProfileRequest.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .build());
        log.info("Saga Forward Step: Profile created for Keycloak user ID: {}", userId);

        // Only after the profile exists: ask notification-service to send the welcome email
        if (!StringUtils.hasText(email)) {
            log.warn("REGISTER event of user {} has no email, skipping welcome email", userId);
            return;
        }
        NotificationEvent welcomeEvent = NotificationEvent.builder()
                .eventId("welcome-" + userId)
                .recipient(email)
                .params(Map.of("name", (firstName + " " + (lastName != null ? lastName : "")).trim()))
                .build();
        // join() so a failed send throws and the record is retried (async failures would be lost otherwise)
        kafkaTemplate.send(WELCOME_TOPIC, userId, welcomeEvent).join();
        log.info("Published welcome-blog event {} for user ID: {}", welcomeEvent.getEventId(), userId);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private static String localPart(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
