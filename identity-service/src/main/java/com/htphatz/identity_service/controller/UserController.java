package com.htphatz.identity_service.controller;

import com.htphatz.identity_service.dto.response.APIResponse;
import com.htphatz.identity_service.dto.response.PageDto;
import com.htphatz.identity_service.dto.response.UserResponse;
import com.htphatz.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("me")
    public APIResponse<UserResponse> getCurrentInformation() {
        UserResponse result = userService.getMyInfo();
        return APIResponse.<UserResponse>builder().result(result).build();
    }

    @GetMapping
    public APIResponse<PageDto<UserResponse>> getAllUsers(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        PageDto<UserResponse> result = userService.getAllUsers(pageNumber, pageSize);
        return APIResponse.<PageDto<UserResponse>>builder().result(result).build();
    }

    @GetMapping("{id}")
    public APIResponse<UserResponse> getUserById(@PathVariable("id") String id) {
        UserResponse result = userService.getUserById(id);
        return APIResponse.<UserResponse>builder().result(result).build();
    }
}
