package com.htphatz.profile_service.mapper;

import com.htphatz.profile_service.dto.request.ProfileRequest;
import com.htphatz.profile_service.dto.response.ProfileResponse;
import com.htphatz.profile_service.entity.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    Profile toProfile(ProfileRequest request);
    ProfileResponse toProfileResponse(Profile entity);
}
