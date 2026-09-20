package com.htphatz.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Email/Username is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
