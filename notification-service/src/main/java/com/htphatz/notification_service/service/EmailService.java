package com.htphatz.notification_service.service;

import com.htphatz.event.NotificationEvent;
import com.htphatz.notification_service.dto.request.EmailReq;
import com.htphatz.notification_service.dto.request.ParamReq;
import com.htphatz.notification_service.dto.request.ToReq;
import com.htphatz.notification_service.repository.httpclient.BrevoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
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

    @KafkaListener(topics = "welcome-blog", groupId = "notification-group")
    public void sendEmail(NotificationEvent notificationEvent) {
        ToReq to = ToReq.builder()
                .email(notificationEvent.getRecipient())
                .build();

        ParamReq params = ParamReq.builder()
                .email(notificationEvent.getRecipient())
                .build();

        EmailReq request = EmailReq.builder()
                .to(List.of(to))
                .templateId(templateId)
                .params(params)
                .build();

        brevoClient.sendEmail(apiKey, request);
    }
}
