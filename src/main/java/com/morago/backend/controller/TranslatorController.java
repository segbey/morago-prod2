package com.morago.backend.controller;

import com.morago.backend.dto.billing.withdrawal.CreateWithdrawalRequest;
import com.morago.backend.dto.translator.*;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.user.UserService;
import com.morago.backend.dto.translator.TranslatorAvailabilityRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translators")
@RequiredArgsConstructor
public class TranslatorController {
    private final TranslatorProfileService translatorProfileService;
    private final UserService userService;

    @PostMapping("/withdrawal-request")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<Long> requestWithdrawal(
            Authentication authentication,
            @Valid @RequestBody CreateWithdrawalRequest withdrawalRequest) {
        Long userId = userService.getCurrentUser().getId();
        Long withdrawalId = translatorProfileService.requestTranslatorWithdrawal(userId, withdrawalRequest);
        return ResponseEntity.ok(withdrawalId);
    }

    @PutMapping("/update/profile")
    @PreAuthorize("hasAnyRole('TRANSLATOR','ADMIN')")
    public ResponseEntity<UserUpdateProfileResponseDto> updateMyProfile(
            @Valid @RequestBody UserUpdateProfileRequestDto dto
    ) {
        UserUpdateProfileResponseDto response = userService.updateMyProfile(dto);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/update/online-status")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<TranslatorAvailabilityResponseDto> updateOnlineStatus(
            @Valid @RequestBody TranslatorAvailabilityRequestDto dto
    ) {
        TranslatorAvailabilityResponseDto response = translatorProfileService.updateOnlineStatus(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/languages")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<TranslatorLanguagesResponseDto> updateLanguages(
            @Valid @RequestBody TranslatorLanguagesRequestDto dto
    ) {
        TranslatorLanguagesResponseDto response = translatorProfileService.updateLanguages(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/themes")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<TranslatorThemesResponseDto> updateThemes(
            @Valid @RequestBody TranslatorThemesUpdateRequest dto
    ) {
        TranslatorThemesResponseDto response = translatorProfileService.updateThemes(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/korean-level")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<TranslatorKoreanLevelResponseDto> updateKoreanLevel(
            @Valid @RequestBody TranslatorKoreanLevelRequestDto dto
    ) {
        TranslatorKoreanLevelResponseDto response = translatorProfileService.updateKoreanLevel(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/basic-info")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<TranslatorBasicInfoResponseDto> updateBasicInfo(
            @Valid @RequestBody TranslatorBasicInfoRequestDto dto
    ) {
        TranslatorBasicInfoResponseDto response = translatorProfileService.updateBasicInfo(dto);
        return ResponseEntity.ok(response);
    }
}