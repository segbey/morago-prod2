package com.morago.backend.service.profile;

import com.morago.backend.dto.UserProfileDto;
import com.morago.backend.dto.user.UserResponseDto;
import com.morago.backend.entity.UserProfile;

import java.util.List;
import java.util.Optional;

public interface UserProfileService {
    UserProfile findByIdOrThrow(Long profileId);
    UserProfile findByUserIdOrThrow(Long userId);
    List<UserProfileDto> getAllUserProfiles();
}
