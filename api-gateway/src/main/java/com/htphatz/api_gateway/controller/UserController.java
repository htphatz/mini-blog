package com.htphatz.api_gateway.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserController {

    @GetMapping("/me")
    public Mono<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            Map<String, Object> anonymous = new HashMap<>();
            anonymous.put("authenticated", false);
            anonymous.put("message", "User is not logged in");
            return Mono.just(anonymous);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("authType", authentication.getClass().getSimpleName());
        userInfo.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            if (oauth2Token.getPrincipal() instanceof OidcUser oidcUser) {
                userInfo.put("userId", oidcUser.getSubject());
                userInfo.put("username", oidcUser.getPreferredUsername());
                userInfo.put("email", oidcUser.getEmail());
                userInfo.put("fullName", oidcUser.getFullName());
                userInfo.put("claims", oidcUser.getClaims());
            } else {
                userInfo.put("userId", oauth2Token.getName());
                userInfo.put("attributes", oauth2Token.getPrincipal().getAttributes());
            }
        } else if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            userInfo.put("userId", jwtAuth.getToken().getSubject());
            userInfo.put("username", jwtAuth.getToken().getClaimAsString("preferred_username"));
            userInfo.put("email", jwtAuth.getToken().getClaimAsString("email"));
            userInfo.put("claims", jwtAuth.getToken().getClaims());
        }

        return Mono.just(userInfo);
    }
}
