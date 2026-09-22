package com.htphatz.identity_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    WELCOME_BLOG("welcome-blog");

    private final String topic;
}
