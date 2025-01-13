package com.htphatz.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RoleRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Permissions is required")
    private Set<String> permissions;
}
