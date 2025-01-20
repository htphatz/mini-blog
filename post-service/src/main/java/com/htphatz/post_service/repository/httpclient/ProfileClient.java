package com.htphatz.post_service.repository.httpclient;

import com.htphatz.post_service.dto.response.ProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service", url = "${services.profile.url}")
public interface ProfileClient {

    @GetMapping(value = "/internal/users/{userId}")
    ProfileResponse getByUserId(@PathVariable("userId") String userId);
}
