package com.htphatz.api_gateway.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntrospectResponse {
    private boolean isValid;
}
