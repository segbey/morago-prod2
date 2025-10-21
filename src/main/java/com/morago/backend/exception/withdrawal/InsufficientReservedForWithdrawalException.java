package com.morago.backend.exception.withdrawal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.math.BigDecimal;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InsufficientReservedForWithdrawalException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final Long withdrawalId;
    private final BigDecimal required; // amt
    private final BigDecimal reserved; // u.getReserved()

    public InsufficientReservedForWithdrawalException(Long userId, Long withdrawalId,
                                                      BigDecimal required, BigDecimal reserved) {
        super("Insufficient reserved funds for withdrawal: userId=" + userId +
                ", withdrawalId=" + withdrawalId +
                ", required=" + required + ", reserved=" + reserved);
        this.userId = userId;
        this.withdrawalId = withdrawalId;
        this.required = required;
        this.reserved = reserved;
    }
}