package com.htphatz.post_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final String clientId;

    public KeycloakRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Realm roles -> ROLE_*
        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            roles.forEach(role -> {
                String name = role.toString();
                authorities.add(new SimpleGrantedAuthority(name.startsWith("ROLE_") ? name : "ROLE_" + name));
            });
        }

        // 2. Client roles of this service = permissions (kept as-is, no ROLE_ prefix)
        Map<String, Object> resourceAccess = (Map<String, Object>) jwt.getClaims().get("resource_access");
        if (resourceAccess != null && resourceAccess.get(clientId) instanceof Map<?, ?> client
                && client.get("roles") instanceof List<?> permissions) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.toString())));
        }

        return authorities;
    }
}
