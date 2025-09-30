package com.morago.backend.repository;

import com.morago.backend.entity.PasswordReset;
import com.morago.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByToken(String token);

    Optional<PasswordReset> findByPhoneAndResetCodeAndUsedFalseAndExpiresAtAfter(
            String phone, Integer resetCode, LocalDateTime now);

    Optional<PasswordReset> findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            String phone, LocalDateTime now);

    Optional<PasswordReset> findByPhoneAndResetCodeAndUsedFalseAndCodeVerifiedFalseAndExpiresAtAfter(
            String phone, Integer resetCode, LocalDateTime now);

    Optional<PasswordReset> findTopByPhoneAndUsedFalseAndCodeVerifiedFalseAndExpiresAtAfterOrderByIdDesc(
            String phone, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update PasswordReset pr
              set pr.used = true, pr.expiresAt = :now
            where pr.user = :user
              and pr.used = false
           """)
    int invalidateAllActiveByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}