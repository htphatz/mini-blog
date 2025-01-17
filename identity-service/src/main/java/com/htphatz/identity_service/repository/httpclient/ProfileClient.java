package com.htphatz.identity_service.repository.httpclient;

import com.htphatz.identity_service.config.AuthenticationRequestInterceptor;
import com.htphatz.identity_service.dto.request.ProfileRequest;
import com.htphatz.identity_service.dto.response.ProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "profile-service",
        url = "${services.profile.url}",
        configuration = { AuthenticationRequestInterceptor.class }
)
public interface ProfileClient {

    @PostMapping(value = "internal/users", produces = MediaType.APPLICATION_JSON_VALUE)
    ProfileResponse createProfile(@RequestBody ProfileRequest request);
}
