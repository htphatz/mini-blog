package com.htphatz.profile_service.controller;

import com.htphatz.profile_service.dto.request.ProfileRequest;
import com.htphatz.profile_service.dto.response.ProfileResponse;
import com.htphatz.profile_service.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileInternalController {
    private final ProfileService profileService;

    @PostMapping("internal/users")
    public ProfileResponse createProfileInternal(@Valid @RequestBody ProfileRequest request) {
        return profileService.createProfile(request);
    }
}
