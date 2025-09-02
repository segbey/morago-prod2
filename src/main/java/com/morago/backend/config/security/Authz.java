package com.morago.backend.config.security;

import com.morago.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component("authz")
@RequiredArgsConstructor
public class Authz {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isSelf(Long targetUserId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || targetUserId == null) return false;

        String username = auth.getName();
        if (username == null) return false;

        return userRepository.findByUsername(username)
                .map(u -> targetUserId.equals(u.getId()))
                .orElse(false);
    }
}