package com.htphatz.identity_service.mapper;

import com.htphatz.identity_service.dto.request.RegisterRequest;
import com.htphatz.identity_service.dto.response.UserResponse;
import com.htphatz.identity_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "roles", ignore = true)
    User toUser(RegisterRequest request);

    UserResponse toUserResponse(User user);
}
