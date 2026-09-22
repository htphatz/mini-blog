package com.htphatz.notification_service.service;

import com.htphatz.event.NotificationEvent;
import com.htphatz.notification_service.document.ProcessedEvent;
import com.htphatz.notification_service.dto.request.EmailReq;
import com.htphatz.notification_service.dto.request.ParamReq;
import com.htphatz.notification_service.dto.request.ToReq;
import com.htphatz.notification_service.repository.ProcessedEventRepository;
import com.htphatz.notification_service.repository.httpclient.BrevoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final BrevoClient brevoClient;
    private final ProcessedEventRepository processedEventRepository;

    @Value(value = "${brevo.api-key}")
    private String apiKey;

    @Value(value = "${brevo.template-id}")
    private Integer templateId;

    /**
     * Idempotent Consumer: Checks if eventId has already been recorded in MongoDB.
     * If already present, skips processing to prevent duplicate emails caused by Kafka retries.
     */
    @KafkaListener(topics = "welcome-blog", groupId = "notification-group")
    public void sendEmail(NotificationEvent notificationEvent) {
        String eventId = notificationEvent.getEventId();

        // 1. Check idempotency: Ignore duplicate messages
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.warn("[Idempotent Consumer] Duplicate event detected: {}. Skipping email delivery.", eventId);
            return;
        }

        log.info("Processing welcome-blog event id: {} for recipient: {}", eventId, notificationEvent.getRecipient());

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

        // 2. Mark event as processed in MongoDB
        if (eventId != null) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .id(eventId)
                    .processedAt(LocalDateTime.now())
                    .build());
            log.info("[Idempotent Consumer] Successfully recorded event {} as processed in MongoDB", eventId);
        }
    }
}
