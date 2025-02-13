package com.htphatz.notification_service.repository.httpclient;

import com.htphatz.notification_service.dto.request.EmailReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "brevo-client", url = "https://api.brevo.com/v3/smtp/email")
public interface BrevoClient {
    @PostMapping
    void sendEmail(@RequestHeader(value = "api-key") String apiKey, @RequestBody EmailReq request);
}

