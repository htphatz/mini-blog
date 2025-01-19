package com.htphatz.post_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PostResponse {
    private String id;
    private String userId;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
