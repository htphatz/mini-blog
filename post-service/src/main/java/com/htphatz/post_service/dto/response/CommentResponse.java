package com.htphatz.post_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CommentResponse {
    private String id;
    private String postId;
    private String content;
    private Instant createAt;
    private Instant updatedAt;
}
