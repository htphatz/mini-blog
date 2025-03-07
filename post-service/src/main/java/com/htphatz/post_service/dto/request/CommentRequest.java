package com.htphatz.post_service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentRequest {
    private String id;
    private String postId;
    private String userId;
    private String content;
}
