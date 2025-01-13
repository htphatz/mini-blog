package com.htphatz.identity_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IntrospectResponse {
    private boolean isValid;
}
