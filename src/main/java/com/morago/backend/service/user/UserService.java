package com.morago.backend.service.user;

import com.morago.backend.dto.password.ChangePasswordRequestDto;
import com.morago.backend.dto.user.UserRegistrationRequestDto;
import com.morago.backend.dto.user.UserRegistrationResponseDto;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.entity.User;

public interface UserService {
    User findByUsernameOrThrow(String username);
    User findByIdOrThrow(Long id);
    User getCurrentUser();

    UserRegistrationResponseDto registerUser(UserRegistrationRequestDto dto);
    UserRegistrationResponseDto registerTranslator(UserRegistrationRequestDto dto);
    UserUpdateProfileResponseDto updateProfile(Long userId, UserUpdateProfileRequestDto dto);

    void changePassword(Long userId, ChangePasswordRequestDto dto);


    UserRegistrationResponseDto createUser(UserRegistrationRequestDto dto);
    void deleteUser(Long id);




}
