package com.htphatz.api_gateway.controller;

import com.htphatz.api_gateway.dto.response.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class UserController {

    @GetMapping("/me")
    public Mono<UserInfo> me(@AuthenticationPrincipal OidcUser user) {
        log.info("id_token claims: {}", user.getClaims().keySet());

        return Mono.just(new UserInfo(
                user.getSubject(),
                user.getPreferredUsername(),
                user.getEmail(),
                user.getFullName()));
    }
}
