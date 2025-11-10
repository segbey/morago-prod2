package com.morago.backend.service.profile;

import com.morago.backend.entity.UserProfile;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfile findByIdOrThrow(Long profileId) {
        return userProfileRepository.findById(profileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("UserProfile not found with id " + profileId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile findByUserIdOrThrow(Long userId) {
        return userProfileRepository.findByUser_Id(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("UserProfile not found for userId " + userId));
    }
}