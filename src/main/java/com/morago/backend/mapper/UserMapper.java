package com.morago.backend.mapper;

import com.morago.backend.dto.user.UserRegistrationRequestDto;
import com.morago.backend.dto.user.UserResponseDto;
import com.morago.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "phoneNumber")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "translatorProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRegistrationRequestDto dto);

    @Mapping(target = "phoneNumber", source = "username")
    UserResponseDto toResponseDto(User user);
}
