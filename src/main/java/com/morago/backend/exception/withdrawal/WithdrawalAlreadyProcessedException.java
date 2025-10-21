package com.morago.backend.exception.withdrawal;

import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
public class WithdrawalAlreadyProcessedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public WithdrawalAlreadyProcessedException(Long withdrawalId, String status) {
        super("Withdrawal already processed: id=" + withdrawalId + ", status=" + status);
    }
}