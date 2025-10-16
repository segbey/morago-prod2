package com.morago.backend.controller;

import com.morago.backend.dto.billing.withdrawal.CreateWithdrawalRequest;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDecisionRequest;
import com.morago.backend.mapper.WithdrawalMapper;
import com.morago.backend.service.withdrawal.WithdrawalService;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Withdrawals",
        description = "Withdrawal management. Access: [USER, TRANSLATOR] for creating requests; [ADMIN] for approval/decision."
)
@RestController
@RequestMapping("/api/withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {
    private final WithdrawalService withdrawalService;
    private final WithdrawalMapper withdrawalMapper;

   /* @Operation(
            summary = "Request withdrawal",
            description = "Access: [USER, TRANSLATOR]\nSubmit a new withdrawal request."
    )
    @ApiResponse(responseCode = "200", description = "Withdrawal request created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','TRANSLATOR')")
    public ResponseEntity<WithdrawalDto> request(@RequestBody @Valid CreateWithdrawalRequest dto) {
        Long id = withdrawalService.requestWithdrawal(
                dto.userId(), dto.accountNumber(), dto.accountHolder(), dto.nameOfBank(), dto.wonAmount()
        );
        var w = withdrawalService.findByIdOrThrow(id);
        return ResponseEntity.ok(withdrawalMapper.toDto(w));
    }*/

    @Operation(
            summary = "Decide withdrawal",
            description = "Access: [ADMIN]\nApprove or reject a withdrawal request."
    )
    @ApiResponse(responseCode = "204", description = "Decision applied")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    @ApiResponse(responseCode = "404", description = "Withdrawal not found")
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> decide(@PathVariable Long id, @RequestBody WithdrawalDecisionRequest dto) {
        withdrawalService.decideWithdrawal(id, dto.approve(), dto.adminNote());
        return ResponseEntity.noContent().build();
    }
}