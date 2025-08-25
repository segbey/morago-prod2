package com.morago.backend.security;

import com.morago.backend.entity.User;
import com.morago.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authz")
public class Authz {
    private final UserRepository userRepository;

    public Authz(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSelf(Long targetUserId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        Object principal = auth.getPrincipal();

        if (principal instanceof User u) {
            return targetUserId != null && targetUserId.equals(u.getId());
        }

        String username = (principal instanceof org.springframework.security.core.userdetails.User su)
                ? su.getUsername()
                : (principal instanceof String s ? s : null);

        if (username == null) return false;

        return userRepository.findByUsername(username)
                .map(u -> targetUserId != null && targetUserId.equals(u.getId()))
                .orElse(false);
    }
}
