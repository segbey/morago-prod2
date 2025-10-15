package com.morago.backend.service.deposit;

import com.morago.backend.entity.Deposit;

import java.math.BigDecimal;

public interface DepositService {
    void authorizeCallStartByTheme(Long clientId, String callId, Long themeId);
    Long createDeposit(Long userId, String accountHolder, String nameOfBank, BigDecimal wonAmount);
    void confirmDeposit(Long depositId);
    void chargeCallAndPay(Long clientId, Long interpreterId, String callId, BigDecimal wonAmount);
    Deposit findByIdOrThrow(Long id);
}
