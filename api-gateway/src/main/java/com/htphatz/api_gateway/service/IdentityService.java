package com.htphatz.api_gateway.service;

import com.htphatz.api_gateway.dto.request.IntrospectRequest;
import com.htphatz.api_gateway.dto.response.APIResponse;
import com.htphatz.api_gateway.dto.response.IntrospectResponse;
import com.htphatz.api_gateway.repository.webclient.IdentityClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final IdentityClient identityClient;

    public Mono<APIResponse<IntrospectResponse>> introspect(String token) {
        IntrospectRequest request = IntrospectRequest.builder()
                .token(token)
                .build();
        return identityClient.introspect(request);
    }
}
