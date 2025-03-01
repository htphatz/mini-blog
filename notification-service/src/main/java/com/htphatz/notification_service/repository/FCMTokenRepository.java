package com.htphatz.notification_service.repository;

import com.htphatz.notification_service.document.FCMToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FCMTokenRepository extends MongoRepository<FCMToken, String> {
    @Query(value = "{ 'user_id': ?0 }")
    Optional<FCMToken> findByUserId(@Param("userId") String userId);
}
