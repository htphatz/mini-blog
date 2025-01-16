package com.htphatz.api_gateway.repository.webclient;

import com.htphatz.api_gateway.dto.request.IntrospectRequest;
import com.htphatz.api_gateway.dto.response.APIResponse;
import com.htphatz.api_gateway.dto.response.IntrospectResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

public interface IdentityClient {

    @PostExchange(url = "/auth/introspect")
    Mono<APIResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request);
}
