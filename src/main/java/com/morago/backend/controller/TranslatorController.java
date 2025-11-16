package com.morago.backend.controller;
import com.morago.backend.dto.billing.withdrawal.CreateWithdrawalRequest;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/translators")
@RequiredArgsConstructor
public class TranslatorController {
    private final TranslatorProfileService translatorProfileService;
    private final UserService userService;

    @Operation(
            summary = "Request withdrawal",
            description = "Submit a withdrawal request for translator earnings.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Withdrawal request submitted successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid withdrawal request"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Translator access required")
            }
    )
    @PostMapping("/withdrawal-request")
    @PreAuthorize("hasRole('TRANSLATOR')")
    public ResponseEntity<Long> requestWithdrawal(
            Authentication authentication,
            @Valid @RequestBody CreateWithdrawalRequest withdrawalRequest) {
        Long userId = userService.getCurrentUser().getId();
        Long withdrawalId = translatorProfileService.requestTranslatorWithdrawal(userId, withdrawalRequest);
        return ResponseEntity.ok(withdrawalId);
    }

    @Operation(
            summary = "Update my profile",
            description = "Update profile of currently authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @PutMapping("/update/profile")
    @PreAuthorize("hasAnyRole('TRANSLATOR','ADMIN')")
    public ResponseEntity<UserUpdateProfileResponseDto> updateMyProfile(
            @Valid @RequestBody UserUpdateProfileRequestDto dto
    ) {
        UserUpdateProfileResponseDto response = userService.updateMyProfile(dto);
        return ResponseEntity.ok(response);
    }
}