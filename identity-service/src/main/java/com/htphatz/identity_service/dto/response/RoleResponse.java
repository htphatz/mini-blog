package com.htphatz.identity_service.dto.response;

import com.htphatz.identity_service.entity.Permission;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RoleResponse {
    private String name;
    private Set<Permission> permissions;
}
