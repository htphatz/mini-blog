package com.htphatz.notification_service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParamReq {
    private String email;
}
