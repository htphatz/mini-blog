package com.htphatz.post_service.dto.response;

import com.htphatz.post_service.document.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class PostResponse {
    private String id;
    private String userId;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Comment> comments;
}
