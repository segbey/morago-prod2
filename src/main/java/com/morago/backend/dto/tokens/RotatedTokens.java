package com.morago.backend.dto.tokens;

import java.time.Instant;

public record RotatedTokens(String newAccessToken, String newRefreshToken, Instant refreshExpiresAt) {
}
