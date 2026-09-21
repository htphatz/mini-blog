package com.htphatz.api_gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserContextFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String userId = null;
                    String username = null;

                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        userId = jwtAuth.getToken().getSubject();
                        username = jwtAuth.getToken().getClaimAsString("preferred_username");
                    } else if (auth instanceof OAuth2AuthenticationToken oauth2Token) {
                        if (oauth2Token.getPrincipal() instanceof OidcUser oidcUser) {
                            userId = oidcUser.getSubject();
                            username = oidcUser.getPreferredUsername();
                        } else {
                            userId = oauth2Token.getName();
                            username = (String) oauth2Token.getPrincipal().getAttributes().get("preferred_username");
                        }
                    }

                    if (userId != null || username != null) {
                        ServerHttpRequest request = exchange.getRequest().mutate()
                                .header("X-User-Id", userId != null ? userId : "")
                                .header("X-Username", username != null ? username : "")
                                .build();
                        return chain.filter(exchange.mutate().request(request).build());
                    }

                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return 10;
    }
}