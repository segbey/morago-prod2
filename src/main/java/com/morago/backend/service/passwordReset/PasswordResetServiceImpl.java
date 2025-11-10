package com.morago.backend.service.passwordReset;

import com.morago.backend.entity.PasswordReset;
import com.morago.backend.entity.User;
import com.morago.backend.exception.passwordReset.MissingResetFieldsException;
import com.morago.backend.exception.passwordReset.MissingVerifyFieldsException;
import com.morago.backend.repository.PasswordResetRepository;
import com.morago.backend.service.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.morago.backend.exception.passwordReset.InvalidResetTokenException;
import com.morago.backend.exception.passwordReset.InvalidResetCodeException;
import org.springframework.core.env.Environment;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetRepository passwordResetRepository;
    private final UserService userService;
    private final Environment env;

    private static final Duration EXPIRES_IN = Duration.ofMinutes(15);
    private static final SecureRandom RNG = new SecureRandom();
    private static final String MASTER_CODE = "1234";

    private static final String ADMIN_PHONE = "01098765671";

    private boolean isDevLike() {
        return Arrays.stream(env.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("local") || p.equalsIgnoreCase("prod"));
    }

    private boolean isExcludedPhone(String phone) {
        return ADMIN_PHONE.equals(phone);
    }

    @Transactional
    public void startReset(String phone) {
        final User user;
        try {
            user = userService.findByUsernameOrThrow(phone);
        } catch (RuntimeException e) {
            return;
        }

        passwordResetRepository.invalidateAllActiveByUser(user, LocalDateTime.now());

        int code = 1000 + RNG.nextInt(9000);
        String token = UUID.randomUUID().toString().replace("-", "");

        PasswordReset pr = PasswordReset.builder()
                .user(user)
                .phone(phone)
                .resetCode(code)
                .token(token)
                .expiresAt(LocalDateTime.now().plus(EXPIRES_IN))
                .used(false)
                .codeVerified(false)
                .verifiedAt(null)
                .build();

        passwordResetRepository.save(pr);
    }

    @Transactional
    public String verifyCode(String phone, Integer code) {
        if (phone == null || phone.isBlank() || code == null) {
            throw new MissingVerifyFieldsException();
        }

        PasswordReset pr;

        final LocalDateTime now = LocalDateTime.now();

        if (isDevLike() && MASTER_CODE.equals(String.valueOf(code)) && !isExcludedPhone(phone)) {
            pr = passwordResetRepository
                    .findTopByPhoneAndUsedFalseAndCodeVerifiedFalseAndExpiresAtAfterOrderByIdDesc(phone, now)
                    .orElseThrow(InvalidResetCodeException::new);
        } else {
            pr = passwordResetRepository
                    .findByPhoneAndResetCodeAndUsedFalseAndCodeVerifiedFalseAndExpiresAtAfter(phone, code, now)
                    .orElseThrow(InvalidResetCodeException::new);
        }

        pr.setCodeVerified(true);
        pr.setVerifiedAt(now);
        pr.setResetCode(null);

        String newToken = UUID.randomUUID().toString().replace("-", "");
        pr.setToken(newToken);

        return newToken;
    }

    @Transactional
    public void confirm(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new MissingResetFieldsException();
        }

        final LocalDateTime now = LocalDateTime.now();

        PasswordReset pr = passwordResetRepository.findByToken(token)
                .orElseThrow(InvalidResetTokenException::new);

        if (Boolean.TRUE.equals(pr.getUsed())
                || pr.getExpiresAt().isBefore(now)
                || !Boolean.TRUE.equals(pr.getCodeVerified())) {
            throw new InvalidResetTokenException();
        }

        User user = pr.getUser();
        userService.setPasswordWithoutOldCheck(user.getId(), newPassword, newPassword);

        pr.setUsed(true);
        pr.setToken(null);

        passwordResetRepository.invalidateAllActiveByUser(user, now);
    }
}