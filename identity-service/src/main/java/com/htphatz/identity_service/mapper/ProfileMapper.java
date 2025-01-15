package com.htphatz.identity_service.mapper;

import com.htphatz.identity_service.dto.request.ProfileRequest;
import com.htphatz.identity_service.dto.request.RegisterRequest;
import com.htphatz.identity_service.dto.response.ProfileResponse;
import com.htphatz.identity_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileRequest toProfileRequest(RegisterRequest request);

    @Mapping(source = "id", target = "userId")
    ProfileResponse toProfileResponse(User entity);
}
