package com.htphatz.notification_service.service;

import com.htphatz.notification_service.dto.request.EmailReq;
import com.htphatz.notification_service.dto.request.ParamReq;
import com.htphatz.notification_service.dto.request.ToReq;
import com.htphatz.notification_service.repository.httpclient.BrevoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final BrevoClient brevoClient;

    @Value(value = "${brevo.api-key}")
    private String apiKey;

    @Value(value = "${brevo.template-id}")
    private Integer templateId;

//    @KafkaListener(topics = "order-confirmed", groupId = "my-group")
    public void sendEmail() {
        ToReq to = ToReq.builder()
                .email("phat@yopmail.com")
                .build();

        ParamReq params = ParamReq.builder()
                .email("phat@yopmail.com")
                .build();

        EmailReq request = EmailReq.builder()
                .to(List.of(to))
                .templateId(templateId)
                .params(params)
                .build();

        brevoClient.sendEmail(apiKey, request);
    }
}
