package com.morago.backend.exception.deposit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvalidDepositAmountException extends RuntimeException {
    private final Long userId;
    private final BigDecimal amount;

    public InvalidDepositAmountException(Long userId, BigDecimal amount) {
        super(buildMessage(userId, amount));
        this.userId = userId;
        this.amount = amount;
    }

    private static String buildMessage(Long userId, BigDecimal amount) {
        return "Invalid deposit amount, must be > 0: userId=" + userId + ", amount=" + amount;
    }
}