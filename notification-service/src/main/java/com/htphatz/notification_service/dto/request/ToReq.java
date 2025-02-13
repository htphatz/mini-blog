package com.htphatz.notification_service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ToReq {
    private String email;
    private String name;
}
