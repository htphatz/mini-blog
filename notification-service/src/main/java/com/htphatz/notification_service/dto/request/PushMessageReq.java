package com.htphatz.notification_service.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class PushMessageReq {
    private String recipientToken;
    private String title;
    private String body;
    private String image;
    private Map<String, String> data;
}
