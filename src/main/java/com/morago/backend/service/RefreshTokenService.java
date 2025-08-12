package com.morago.backend.service;

import com.morago.backend.dto.tokens.JWTResponse;
import com.morago.backend.entity.RefreshToken;
import com.morago.backend.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username, String jwtTokenString);
    Optional<RefreshToken> findByToken(String token);
    boolean isRefreshTokenExpired(RefreshToken token);
    void deleteByUser(User user);
    void deleteByToken(String token);
    JWTResponse refreshToken(String requestRefreshToken);
    void logoutUserByRefreshToken(String refreshTokenStr);
}
