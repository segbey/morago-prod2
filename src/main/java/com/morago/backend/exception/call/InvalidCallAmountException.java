package com.morago.backend.exception.call;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvalidCallAmountException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Long clientId;
    private final Long interpreterId;
    private final String callId;
    private final BigDecimal amount; // исходное значение, которое пришло

    public InvalidCallAmountException(Long clientId, Long interpreterId, String callId, BigDecimal amount) {
        super("Invalid call amount, must be > 0: clientId=" + clientId +
                ", interpreterId=" + interpreterId + ", callId=" + callId +
                ", amount=" + amount);
        this.clientId = clientId;
        this.interpreterId = interpreterId;
        this.callId = callId;
        this.amount = amount;
    }
}