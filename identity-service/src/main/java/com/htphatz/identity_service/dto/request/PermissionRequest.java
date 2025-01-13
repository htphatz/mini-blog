package com.htphatz.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionRequest {

    @NotBlank(message = "Name is required")
    private String name;
}
