package com.morago.backend.dto.withdrawal;

import jakarta.validation.constraints.NotNull;

public record WithdrawalDecisionRequest(
        @NotNull Boolean approve,
        String adminNote
) {}
