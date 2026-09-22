package com.htphatz.notification_service.repository;

import com.htphatz.notification_service.document.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
}
