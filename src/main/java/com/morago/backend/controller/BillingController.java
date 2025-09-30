package com.morago.backend.controller;

import com.morago.backend.dto.billing.deposit.DepositConfirmResponse;
import com.morago.backend.dto.billing.deposit.DepositDto;
import com.morago.backend.dto.call.ChargeCallRequest;
import com.morago.backend.dto.billing.deposit.CreateDepositRequest;
import com.morago.backend.mapper.DepositMapper;
import com.morago.backend.service.deposit.DepositService;
import com.morago.backend.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Billing",
        description = "Billing operations: deposits and charging calls. " +
                "Access: [USER, TRANSLATOR] for deposits; [ADMIN] for confirming deposits and charge-call."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final DepositService depositService;
    private final UserService userService;
    private final DepositMapper depositMapper;

    @Operation(
            summary = "Create deposit",
            description = "Access: [USER, TRANSLATOR]\nCreates a new deposit request for the current user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deposit created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @PostMapping("/deposits")
    @PreAuthorize("hasAnyRole('USER','TRANSLATOR')")
    public ResponseEntity<DepositDto> createDeposit(@RequestBody @Valid CreateDepositRequest req) {
        Long me = userService.getCurrentUserId();
        Long id = depositService.createDeposit(me, req.accountHolder(), req.nameOfBank(), req.wonAmount());
        var dep = depositService.findByIdOrThrow(id);
        return ResponseEntity.ok(depositMapper.toDto(dep));
    }

    @Operation(
            summary = "Confirm deposit",
            description = "Access: [ADMIN]\nConfirms a pending deposit request.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deposit confirmed"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Deposit not found")
            }
    )
    @PostMapping("/deposits/{depositId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepositConfirmResponse> confirmDeposit(@PathVariable Long depositId) {
        depositService.confirmDeposit(depositId);

        var dep = depositService.findByIdOrThrow(depositId);
        var user = userService.findByIdOrThrow(dep.getUser().getId());

        var body = new DepositConfirmResponse(
                dep.getId(),
                user.getId(),
                dep.getWonDecimal(),
                dep.getStatus(),
                user.getBalance(),
                "deposit:" + dep.getId(),
                dep.getUpdatedAt() != null ? dep.getUpdatedAt() : dep.getCreatedAt()
        );
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Charge call and pay",
            description = "Access: [ADMIN]\nCharges a call and processes interpreter payment.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Call charged and payment processed"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
            }
    )
    @PostMapping("/charge-call")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void chargeCall(@RequestBody @Valid ChargeCallRequest dto) {
        depositService.chargeCallAndPay(dto.clientId(), dto.interpreterId(), dto.callId(), dto.wonAmount());
    }
}