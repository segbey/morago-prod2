package com.morago.backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private boolean active;
    private Byte onBoardingStatus;

    private BigDecimal balance;
    private BigDecimal reserved;
    private BigDecimal available;

    private List<String> roles;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserProfileLite userProfile;
    private TranslatorProfileLite translatorProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileLite {
        private Long id;
        private String nickname;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranslatorProfileLite {
        private Long id;
        private String status;
        private Double rating;
    }
}
