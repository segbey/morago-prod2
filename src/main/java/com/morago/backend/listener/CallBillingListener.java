package com.morago.backend.listener;

import com.morago.backend.event.CallEndedEvent;
import com.morago.backend.service.deposit.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class CallBillingListener {
    private final DepositService depositService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT)
    public void onCallEnded(CallEndedEvent e) {
        depositService.chargeCallAndPay(e.clientId(), e.interpreterId(), e.callId(), e.amountWon());
    }
}
