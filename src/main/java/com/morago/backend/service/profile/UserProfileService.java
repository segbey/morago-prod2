package com.morago.backend.service.profile;

import com.morago.backend.entity.UserProfile;

public interface UserProfileService {
    UserProfile findByIdOrThrow(Long profileId);
    UserProfile findByUserIdOrThrow(Long userId);
}