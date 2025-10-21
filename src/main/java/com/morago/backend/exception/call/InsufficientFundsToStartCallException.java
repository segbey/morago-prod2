package com.morago.backend.exception.call;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InsufficientFundsToStartCallException extends RuntimeException {
    private final Long clientId;
    private final Long themeId;
    private final String callId;
    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientFundsToStartCallException(
            Long clientId,
            Long themeId,
            String callId,
            BigDecimal required,
            BigDecimal available
    ) {
        super(buildMessage(clientId, themeId, callId, required, available));
        this.clientId = clientId;
        this.themeId = themeId;
        this.callId = callId;
        this.required = required;
        this.available = available;
    }

    private static String buildMessage(Long clientId, Long themeId, String callId,
                                       BigDecimal required, BigDecimal available) {
        return "Insufficient funds to start call. " +
                "clientId=" + clientId +
                ", themeId=" + themeId +
                ", callId=" + callId +
                ", required=" + required +
                ", available=" + available;
    }
}