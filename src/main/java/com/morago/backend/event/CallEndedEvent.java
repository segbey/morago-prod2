package com.morago.backend.event;

import java.math.BigDecimal;

public record CallEndedEvent(Long clientId, Long interpreterId, String callId, BigDecimal amountWon) {}