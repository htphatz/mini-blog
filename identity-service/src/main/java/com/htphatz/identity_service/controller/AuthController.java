package com.htphatz.identity_service.controller;

import com.htphatz.identity_service.dto.request.IntrospectRequest;
import com.htphatz.identity_service.dto.request.LoginRequest;
import com.htphatz.identity_service.dto.request.LogoutRequest;
import com.htphatz.identity_service.dto.request.RegisterRequest;
import com.htphatz.identity_service.dto.response.APIResponse;
import com.htphatz.identity_service.dto.response.IntrospectResponse;
import com.htphatz.identity_service.dto.response.LoginResponse;
import com.htphatz.identity_service.dto.response.UserResponse;
import com.htphatz.identity_service.service.AuthService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeyLengthException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("register")
    public APIResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse result = authService.register(request);
        return APIResponse.<UserResponse>builder().result(result).build();
    }

    @PostMapping("login")
    public APIResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) throws KeyLengthException {
        LoginResponse result = authService.login(request);
        return APIResponse.<LoginResponse>builder().result(result).build();
    }

    @PostMapping("introspect")
    public APIResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        IntrospectResponse result = authService.introspect(request);
        return APIResponse.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("logout")
    public APIResponse<Void> logout(@Valid @RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authService.logout(request);
        return APIResponse.<Void>builder().build();
    }
}
