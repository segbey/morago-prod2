package com.morago.backend.config.utils;

import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.time.Instant;

public final class CookieUtils {
    private CookieUtils() {}

    public static final String REFRESH_COOKIE = "refresh_token";

    public static ResponseCookie refreshCookie(String token, Instant expiresAt,
                                               String path, boolean secure, String sameSite) {
        long maxAge = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path(path != null ? path : "/")
                .maxAge(Duration.ofSeconds(maxAge))
                .sameSite(sameSite)
                .build();
    }

    public static ResponseCookie deleteRefreshCookie(String path, boolean secure, String sameSite) {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path(path != null ? path : "/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }
}