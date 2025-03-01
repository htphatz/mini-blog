package com.htphatz.notification_service.controller;

import com.htphatz.notification_service.dto.request.FCMTokenReq;
import com.htphatz.notification_service.service.FCMTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("fcm")
@RequiredArgsConstructor
public class FCMTokenController {
    private final FCMTokenService fcmTokenService;

    @PostMapping
    public String createFCMToken(@RequestBody FCMTokenReq request) {
        fcmTokenService.createFCMToken(request);
        return "Create FCM Token successfully";
    }
}
