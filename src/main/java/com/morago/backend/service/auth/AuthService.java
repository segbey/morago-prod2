package com.morago.backend.service.auth;

import com.morago.backend.dto.tokens.AuthTokens;
import com.morago.backend.dto.tokens.JWTRequest;

public interface AuthService {
    AuthTokens createAuthToken(JWTRequest authRequest);
}
