package com.morago.backend.service;

import com.morago.backend.entity.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    User findByUsernameOrThrow(String username);
}
