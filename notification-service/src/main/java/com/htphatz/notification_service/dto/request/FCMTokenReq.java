package com.htphatz.notification_service.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FCMTokenReq {
    private String userId;
    private String fcmToken;
}
