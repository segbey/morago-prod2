package com.morago.backend.service.deposit;

import com.morago.backend.entity.Deposit;
import com.morago.backend.entity.Money;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.EStatus;
import com.morago.backend.entity.enumFiles.TransactionType;
import com.morago.backend.exception.DepositNotFoundException;
import com.morago.backend.repository.DepositRepository;
import com.morago.backend.service.transaction.TransactionService;
import com.morago.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService{
    private final DepositRepository depositRepo;
    private final UserService userService;
    private final TransactionService txnService;

    @Override
    @Transactional
    public Long createDeposit(Long userId, String accountHolder, String nameOfBank, BigDecimal wonAmount) {
        BigDecimal amt = Money.s2(wonAmount);
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        User user = userService.findByIdOrThrow(userId);

        Deposit dep = Deposit.builder()
                .user(user)
                .accountHolder(accountHolder)
                .nameOfBank(nameOfBank)
                .wonDecimal(amt)
                .coinDecimal(BigDecimal.ZERO)
                .status(EStatus.PENDING)
                .build();

        return depositRepo.save(dep).getId();
    }

    @Override
    @Transactional
    public void confirmDeposit(Long depositId) {
        Deposit dep = depositRepo.findById(depositId).orElseThrow();

        if (dep.getStatus() == EStatus.SUCCESSFUL) return;

        BigDecimal amt = Money.s2(dep.getWonDecimal());
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        User user = userService.findByIdOrThrow(dep.getUser().getId());

        BigDecimal before = user.getBalance();
        user.setBalance(before.add(amt));

        txnService.log(
                user,
                TransactionType.DEPOSIT,
                amt,
                before,
                user.getBalance(),
                "deposit:" + dep.getId(),
                "Deposit confirmed",
                EStatus.SUCCESSFUL
        );

        dep.setStatus(EStatus.SUCCESSFUL);
    }

    @Override
    @Transactional
    public void chargeCallAndPay(Long clientId, Long interpreterId, String callId, BigDecimal wonAmount) {
        if (txnService.existsByCorrelationId(callId)) return;

        BigDecimal amt = Money.s2(wonAmount);
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        User client = userService.findByIdOrThrow(clientId);
        if (client.getAvailable().compareTo(amt) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        BigDecimal cBefore = client.getBalance();
        client.setBalance(cBefore.subtract(amt));
        txnService.log(
                client,
                TransactionType.CALL_DEBIT,
                amt,
                cBefore,
                client.getBalance(),
                callId,
                "Call charge",
                EStatus.SUCCESSFUL
        );

        User interp = userService.findByIdOrThrow(interpreterId);
        BigDecimal iBefore = interp.getBalance();
        interp.setBalance(iBefore.add(amt));
        txnService.log(
                interp,
                TransactionType.CALL_CREDIT,
                amt,
                iBefore,
                interp.getBalance(),
                callId,
                "Call payout",
                EStatus.SUCCESSFUL
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit findByIdOrThrow(Long id) {
        return depositRepo.findById(id)
                .orElseThrow(() -> new DepositNotFoundException(id));
    }
}