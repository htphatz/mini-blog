package com.htphatz.post_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String userId;
}
