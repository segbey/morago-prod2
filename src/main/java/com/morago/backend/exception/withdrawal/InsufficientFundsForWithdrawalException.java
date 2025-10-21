package com.morago.backend.exception.withdrawal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.math.BigDecimal;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InsufficientFundsForWithdrawalException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientFundsForWithdrawalException(Long userId, BigDecimal required, BigDecimal available) {
        super("Insufficient funds for withdrawal. userId=" + userId +
                ", required=" + required + ", available=" + available);
        this.userId = userId;
        this.required = required;
        this.available = available;
    }
}
