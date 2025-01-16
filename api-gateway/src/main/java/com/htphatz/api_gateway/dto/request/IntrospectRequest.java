package com.htphatz.api_gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntrospectRequest {

    @NotBlank(message = "Token is required")
    private String token;
}
