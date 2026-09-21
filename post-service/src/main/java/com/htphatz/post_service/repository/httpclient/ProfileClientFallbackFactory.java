package com.htphatz.post_service.repository.httpclient;

import com.htphatz.post_service.dto.response.ProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ProfileClientFallbackFactory implements FallbackFactory<ProfileClient> {

    @Override
    public ProfileClient create(Throwable cause) {
        log.error("ProfileClient fallback triggered due to: {}", cause.getMessage(), cause);

        return new ProfileClient() {
            @Override
            public ProfileResponse getByUserId(String userId) {
                log.warn("Fallback getByUserId triggered for userId: {}. Underlying cause: {}", userId, cause.getMessage());
                return ProfileResponse.builder()
                        .userId(userId)
                        .firstName("User")
                        .lastName("Unavailable")
                        .phoneNumber("")
                        .address("")
                        .build();
            }

            @Override
            public List<ProfileResponse> getAllProfiles() {
                return List.of();
            }
        };
    }
}
