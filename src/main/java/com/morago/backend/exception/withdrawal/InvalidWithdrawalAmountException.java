package com.morago.backend.exception.withdrawal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.math.BigDecimal;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvalidWithdrawalAmountException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final BigDecimal amount;

    public InvalidWithdrawalAmountException(Long userId, BigDecimal amount) {
        super("Invalid withdrawal amount (> 0 required). userId=" + userId + ", amount=" + amount);
        this.userId = userId;
        this.amount = amount;
    }
}