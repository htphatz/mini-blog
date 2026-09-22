package com.htphatz.identity_service.repository;

import com.htphatz.identity_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();

    List<OutboxEvent> findByAggregateId(String aggregateId);

    void deleteByAggregateId(String aggregateId);
}
