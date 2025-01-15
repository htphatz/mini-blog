package com.htphatz.profile_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String userId;
}
