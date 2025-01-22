package com.htphatz.post_service.dto.response;

import com.htphatz.post_service.enums.ReactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ReactionResponse {
    private String id;
    private String postId;
    private String userId;
    private String displayName;
    private ReactionType reactionType;
    private Instant createdAt;
}
