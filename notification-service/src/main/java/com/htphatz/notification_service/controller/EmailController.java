package com.htphatz.notification_service.controller;

import com.htphatz.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("send")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    @PostMapping("send-email")
    public String sendEmail() {
        return "Send email successfully";
    }
}
