package com.morago.backend.dto.tokens;

import com.morago.backend.dto.user.UserDto;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuthTokens {
    private final String accessToken;
    private final String refreshToken;
    private final Instant refreshExpiresAt;
    private final UserDto user;
}