package com.htphatz.post_service.repository;

import com.htphatz.post_service.document.Reaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends MongoRepository<Reaction, String> {

    @Query(value = "{ 'post_id': ?0, 'user_id': ?1 }")
    Optional<Reaction> findByPostIdAndUserId(String postId, String userId);

    @Query(value = "{ 'post_id': ?0 }")
    List<Reaction> findByPostId(@Param("postId") String postId);

    @Query(value = "{ 'post_id': ?0 }")
    Page<Reaction> findByPostIdPageable(@Param("postId") String postId, Pageable pageable);
}
