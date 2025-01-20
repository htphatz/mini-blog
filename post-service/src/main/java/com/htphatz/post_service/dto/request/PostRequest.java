package com.htphatz.post_service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRequest {
    private String id;
    private String description;
}
