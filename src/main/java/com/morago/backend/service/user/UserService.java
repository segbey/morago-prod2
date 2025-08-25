package com.morago.backend.service.user;


import com.morago.backend.dto.UserProfileDto;
import com.morago.backend.dto.user.UserRegistrationRequestDto;
import com.morago.backend.dto.user.UserResponseDto;
import com.morago.backend.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User findByUsernameOrThrow(String username);
    User findByIdOrThrow(Long id);
    User getCurrentUser();

    UserResponseDto registerUser(UserRegistrationRequestDto dto);
    UserResponseDto registerTranslator(UserRegistrationRequestDto dto);
    UserResponseDto updateProfile(Long userId, UserProfileDto dto);


    UserResponseDto createUser(UserRegistrationRequestDto dto);
    void deleteUser(Long id);




}
