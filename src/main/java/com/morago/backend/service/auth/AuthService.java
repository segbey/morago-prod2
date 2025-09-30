package com.morago.backend.service.auth;

import com.morago.backend.dto.auth.AuthResponse;
import com.morago.backend.dto.tokens.JWTRequest;

public interface AuthService {
    AuthResponse createAuthToken(JWTRequest authRequest);
}
