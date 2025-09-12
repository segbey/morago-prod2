package com.morago.backend.service.user;


import com.morago.backend.dto.user.UserRequestDto;
import com.morago.backend.dto.user.UserResponseDto;
import com.morago.backend.dto.password.ChangePasswordRequestDto;
import com.morago.backend.dto.user.UserRegistrationRequestDto;
import com.morago.backend.dto.user.UserRegistrationResponseDto;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.entity.User;

public interface UserService {
    Optional<User> findByUsername(String username);
    User findByUsernameOrThrow(String username);
    UserResponseDto createUser(UserRequestDto dto);
    UserResponseDto getUser(Long id);
    List<UserResponseDto> getAllUsers();
    UserResponseDto updateUser(Long id, UserRequestDto dto);
    User findByIdOrThrow(Long id);
    User getCurrentUser();
    Long getCurrentUserId();

    UserRegistrationResponseDto registerUser(UserRegistrationRequestDto dto);
    UserRegistrationResponseDto registerTranslator(UserRegistrationRequestDto dto);


    //for admin
    void changePassword(Long userId, ChangePasswordRequestDto dto);
    UserUpdateProfileResponseDto updateProfile(Long userId, UserUpdateProfileRequestDto dto);

    //for 'me'
    UserUpdateProfileResponseDto updateMyProfile(UserUpdateProfileRequestDto dto);
    void changeMyPassword(ChangePasswordRequestDto dto);


    UserRegistrationResponseDto createUser(UserRegistrationRequestDto dto);
    void deleteUser(Long id);
}
