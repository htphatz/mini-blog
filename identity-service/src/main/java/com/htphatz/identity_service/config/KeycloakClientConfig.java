package com.htphatz.identity_service.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakClientConfig {
    @Value("${keycloak.server-url:http://localhost:8484}")
    private String serverUrl;

    @Value("${keycloak.realm:mini-blog-realm}")
    private String realm;

    @Value("${keycloak.client-id:identity-service-client}")
    private String clientId;

    @Value("${keycloak.client-secret:4zt3bb1sT6GhIxCG20Gw4PrvaHD7S1DO}")
    private String clientSecret;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
