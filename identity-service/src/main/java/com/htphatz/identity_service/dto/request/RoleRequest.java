package com.htphatz.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RoleRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Permissions is required")
    private Set<String> permissions;
}
