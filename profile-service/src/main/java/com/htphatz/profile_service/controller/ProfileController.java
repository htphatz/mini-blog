package com.htphatz.profile_service.controller;

import com.htphatz.profile_service.dto.request.ProfileRequest;
import com.htphatz.profile_service.dto.response.APIResponse;
import com.htphatz.profile_service.dto.response.PageDto;
import com.htphatz.profile_service.dto.response.ProfileResponse;
import com.htphatz.profile_service.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping
    public APIResponse<ProfileResponse> createProfile(@Valid @RequestBody ProfileRequest request) {
        ProfileResponse result = profileService.createProfile(request);
        return APIResponse.<ProfileResponse>builder().result(result).build();
    }

    @GetMapping
    public APIResponse<PageDto<ProfileResponse>> getAllProfiles(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        PageDto<ProfileResponse> result = profileService.getAllProfiles(pageNumber, pageSize);
        return APIResponse.<PageDto<ProfileResponse>>builder().result(result).build();
    }

    @GetMapping("{id}")
    public APIResponse<ProfileResponse> getProfileById(@PathVariable("id") String id) {
        ProfileResponse result = profileService.getById(id);
        return APIResponse.<ProfileResponse>builder().result(result).build();
    }

    @DeleteMapping("{id}")
    public APIResponse<Void> deleteProfileById(@PathVariable("id") String id) {
        profileService.deleteProfile(id);
        return APIResponse.<Void>builder().build();
    }
}
