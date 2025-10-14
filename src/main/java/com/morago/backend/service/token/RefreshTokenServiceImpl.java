package com.morago.backend.service.token;

import com.morago.backend.config.utils.JWTProperties;
import com.morago.backend.config.utils.JWTUtils;
import com.morago.backend.dto.tokens.RotatedTokens;
import com.morago.backend.entity.RefreshToken;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.TokenType;
import com.morago.backend.repository.RefreshTokenRepository;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final JWTProperties jwtProperties;
    private final JWTUtils jwtUtils;
    private final TranslatorProfileService translatorProfileService;

    @Override
    public void createRefreshToken(String username, String jwtTokenString) {
        User user = userService.findByUsernameOrThrow(username);
        RefreshToken token = RefreshToken.builder()
                .token(jwtTokenString)
                .user(user)
                .expirationTime(LocalDateTime.now()
                        .plus(Duration.ofMillis(jwtProperties.getRefreshExpirationMs())))
                .build();
        refreshTokenRepository.save(token);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public boolean isRefreshTokenExpired(RefreshToken token) {
        return token.getExpirationTime().isBefore(LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    @Transactional
    public RotatedTokens refreshTokens(String requestRefreshToken) {
        RefreshToken refreshToken = getValidTokenOrThrow(requestRefreshToken);

        User user = refreshToken.getUser();
        String newAccessToken  = jwtUtils.generateAccessToken(user);
        String newRefreshToken = jwtUtils.generateRefreshToken(user);

        deleteByToken(requestRefreshToken);
        createRefreshToken(user.getUsername(), newRefreshToken);

        Instant refreshExp = jwtUtils.getExpirationInstant(newRefreshToken, TokenType.REFRESH);

        return new RotatedTokens(newAccessToken, newRefreshToken, refreshExp);
    }

    @Override
    @Transactional
    public void logout(String username, String refreshTokenNullable) {
        User user = userService.findByUsernameOrThrow(username);

        long before = refreshTokenRepository.countByUser(user);

        if (refreshTokenNullable == null || refreshTokenNullable.isBlank()) {
            refreshTokenRepository.deleteByUser(user);
            log.info("logout: delete ALL tokens for user={}, before={}", username, before);
        } else {
            String token = refreshTokenNullable.trim();

            boolean deletedExact = refreshTokenRepository.findByToken(token)
                    .map(rt -> { refreshTokenRepository.delete(rt); return true; })
                    .orElse(false);

            if (deletedExact) {
                log.info("logout: deleted ONE by exact token. user={}, before={}, tokenLen={}",
                        username, before, token.length());
            } else {
                refreshTokenRepository.deleteByUser(user);
                log.warn("logout: token not found by exact match, deleted ALL. user={}, before={}, tokenLen={}",
                        username, before, token.length());
            }
        }

        long after = refreshTokenRepository.countByUser(user);
        log.info("logout: after={}, deleted={}", after, before - after);

        translatorProfileService.setOnlineStatus(user, false);
    }
}
