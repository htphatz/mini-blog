package com.htphatz.identity_service.dto.response;

import com.htphatz.identity_service.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class UserResponse {
    private String id;
    private String email;
    private Set<Role> roles;
}
