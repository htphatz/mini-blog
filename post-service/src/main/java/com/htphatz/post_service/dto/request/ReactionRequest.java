package com.htphatz.post_service.dto.request;

import com.htphatz.post_service.enums.ReactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReactionRequest {
    private String postId;
    private ReactionType reactionType;
}
