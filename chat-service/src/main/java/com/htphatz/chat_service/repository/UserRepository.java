package com.htphatz.chat_service.repository;

import com.htphatz.chat_service.document.User;
import com.htphatz.chat_service.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    @Query(value = "{ 'user_id': ?0 }")
    Optional<User> findByUserId(@Param("user_id") String userId);

    Page<User> findAllByStatus(Status status, Pageable pageable);
}
