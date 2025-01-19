package com.htphatz.post_service.repository;

import com.htphatz.post_service.document.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    @Query(value = "{ 'post_id': ?0 }")
    List<Comment> findByPostId(@Param("postId") String postId);
}
