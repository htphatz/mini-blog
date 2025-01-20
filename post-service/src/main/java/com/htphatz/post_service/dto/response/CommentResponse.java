package com.htphatz.post_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class CommentResponse {
    private String id;
    private String postId;
    private String userId;
    private String displayName;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
