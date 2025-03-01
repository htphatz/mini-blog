package com.htphatz.notification_service.controller;

import com.htphatz.notification_service.dto.request.PushMessageReq;
import com.htphatz.notification_service.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("push")
@RequiredArgsConstructor
public class PushController {
    private final PushService pushService;

    @PostMapping("push-notification")
    public String pushNotification(@RequestBody PushMessageReq request) {
        return pushService.pushNotification(request);
    }
}
