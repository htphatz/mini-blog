package com.htphatz.identity_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.htphatz.event.NotificationEvent;
import com.htphatz.identity_service.entity.OutboxEvent;
import com.htphatz.identity_service.enums.EventType;
import com.htphatz.identity_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Polling Publisher: Runs every 3 seconds to fetch unprocessed events
     * from outbox_events and publish them to Kafka.
     * Guarantees At-Least-Once Delivery.
     */
    @Scheduled(fixedRate = 3000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox event(s) to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                Object payloadObject;
                if (event.getEventType() == EventType.WELCOME_BLOG) {
                    payloadObject = objectMapper.readValue(event.getPayload(), NotificationEvent.class);
                } else {
                    payloadObject = event.getPayload();
                }

                String topic = event.getEventType().getTopic();
                kafkaTemplate.send(topic, event.getAggregateId(), payloadObject)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event id: {} to topic: {}", event.getId(), topic, ex);
                            } else {
                                log.info("Successfully published outbox event id: {} to topic: {} partition: {}",
                                        event.getId(), topic, result.getRecordMetadata().partition());
                            }
                        });

                event.setProcessed(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error processing outbox event id: {}. Will retry on next poll cycle.", event.getId(), e);
            }
        }
    }
}
