package com.htphatz.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutRequest {
    @NotBlank(message = "Refresh token is required for logout")
    private String refreshToken;
}
