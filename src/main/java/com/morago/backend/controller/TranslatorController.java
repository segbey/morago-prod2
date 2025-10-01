package com.morago.backend.controller;

import com.morago.backend.dto.ThemeDto;
import com.morago.backend.dto.billing.transaction.MyTransactionDto;
import com.morago.backend.dto.billing.withdrawal.CreateWithdrawalRequest;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.entity.Transaction;
import com.morago.backend.mapper.TransactionMapper;
import com.morago.backend.mapper.WithdrawalMapper;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.theme.ThemeService;
import com.morago.backend.service.transaction.TransactionService;
import com.morago.backend.service.user.UserService;
import com.morago.backend.service.withdrawal.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/translators")
@RequiredArgsConstructor
@Tag(name = "Translator", description = "Endpoints for ROLE_TRANSLATOR only")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TRANSLATOR')")
public class TranslatorController {

    private final UserService userService;
    private final ThemeService themeService;
    private final WithdrawalService withdrawalService;
    private final WithdrawalMapper withdrawalMapper;
    private final TranslatorProfileService translatorProfileService;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @Operation(
            summary = "Update translator profile",
            description = "Update translator's profile information (first name, last name, etc.).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserUpdateProfileRequestDto.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                            content = @Content(schema = @Schema(implementation = UserUpdateProfileResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @PutMapping("/profile")
    public ResponseEntity<UserUpdateProfileResponseDto> updateProfile(
            @Valid @RequestBody UserUpdateProfileRequestDto dto
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(dto));
    }

    @Operation(
            summary = "Delete translator profile",
            description = "Soft delete translator profile. Cannot delete if there's outstanding balance or active calls.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Profile deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Cannot delete - outstanding balance or active calls"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @DeleteMapping("/profile")  // ✅ ADD THIS
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile() {
        var user = userService.getCurrentUser();

        if (user.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot delete profile with outstanding balance.");
        }

        userService.deleteUser(user.getId());
    }


    @Operation(
            summary = "Get all available themes",
            description = "Retrieve list of all themes that translators can work with.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Themes retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ThemeDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @GetMapping("/themes")
    public ResponseEntity<List<ThemeDto>> getAllThemes() {
        return ResponseEntity.ok(themeService.getAll());
    }

    @Operation(
            summary = "Request withdrawal",
            description = "Create a withdrawal request to withdraw money from translator's balance. " +
                    "Amount will be reserved until admin approves/rejects the request.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateWithdrawalRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Withdrawal request created successfully",
                            content = @Content(schema = @Schema(implementation = WithdrawalDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data or insufficient funds"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<WithdrawalDto> requestWithdrawal(
            @Valid @RequestBody CreateWithdrawalRequest request
    ) {
        Long userId = userService.getCurrentUserId();

        // Validation checks are performed in the service layer:
        // - Amount must be > 0
        // - Available balance must be >= withdrawal amount

        Long withdrawalId = withdrawalService.requestWithdrawal(
                userId,
                request.accountNumber(),
                request.accountHolder(),
                request.nameOfBank(),
                request.wonAmount()
        );

        var withdrawal = withdrawalService.findByIdOrThrow(withdrawalId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(withdrawalMapper.toDto(withdrawal));
    }

    @Operation(
            summary = "Get withdrawal history",
            description = "Retrieve withdrawal history for the current translator with pagination.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Withdrawal history retrieved successfully",
                            content = @Content(schema = @Schema(implementation = WithdrawalDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @GetMapping("/withdrawals")
    public ResponseEntity<List<WithdrawalDto>> getWithdrawalHistory(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // Long userId = userService.getCurrentUserId();
        // Page<Withdrawal> withdrawals = withdrawalService.getWithdrawalsByUserId(userId, pageable);
        // return ResponseEntity.ok(withdrawals.map(withdrawalMapper::toDto));
        return ResponseEntity.ok(List.of());
    }

    @Operation(
            summary = "Get transaction history",
            description = "Retrieve transaction history for the current translator with pagination.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully",
                            content = @Content(schema = @Schema(implementation = MyTransactionDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @GetMapping("/transactions")
    public ResponseEntity<Page<MyTransactionDto>> getTransactionHistory(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = userService.getCurrentUserId();
        Page<Transaction> transactions = transactionService.history(userId, pageable);
        return ResponseEntity.ok(transactions.map(transactionMapper::toMyDto));
    }

    @Operation(
            summary = "Switch online status",
            description = "Toggle translator's online/offline status. Online translators are visible to users.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(example = "{\"isOnline\": true}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request - isOnline field required"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @PutMapping("/status")
    public ResponseEntity<Map<String, Boolean>> switchStatus(
            @RequestBody Map<String, Boolean> request
    ) {
        Boolean isOnline = request.get("isOnline");
        if (isOnline == null) {
            throw new IllegalArgumentException("isOnline field is required");
        }

        var user = userService.getCurrentUser();
        translatorProfileService.setOnlineStatus(user, isOnline);

        return ResponseEntity.ok(Map.of("isOnline", isOnline));
    }

    @Operation(
            summary = "Get current status",
            description = "Retrieve the current online/offline status of the translator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - TRANSLATOR role required")
            }
    )
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        var user = userService.getCurrentUser();
        if (user.getTranslatorProfile() == null) {
            throw new IllegalStateException("Translator profile not found");
        }

        Boolean isOnline = user.getTranslatorProfile().getIsOnline();
        return ResponseEntity.ok(Map.of(
                "isOnline", isOnline != null ? isOnline : false
        ));

    }
}