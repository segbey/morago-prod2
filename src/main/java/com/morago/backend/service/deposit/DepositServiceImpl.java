package com.morago.backend.service.deposit;

import com.morago.backend.config.utils.ThemePriceUtil;
import com.morago.backend.entity.Deposit;
import com.morago.backend.entity.Money;
import com.morago.backend.entity.Theme;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.EStatus;
import com.morago.backend.entity.enumFiles.TransactionType;
import com.morago.backend.exception.DepositNotFoundException;
import com.morago.backend.exception.call.InsufficientFundsToStartCallException;
import com.morago.backend.exception.call.InvalidCallAmountException;
import com.morago.backend.exception.deposit.InvalidDepositAmountException;
import com.morago.backend.repository.CallRepository;
import com.morago.backend.repository.DepositRepository;
import com.morago.backend.service.debtor.DebtorService;
import com.morago.backend.service.debtor.DebtorServiceImpl;
import com.morago.backend.service.theme.ThemeService;
import com.morago.backend.service.transaction.TransactionService;
import com.morago.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private final DepositRepository depositRepo;
    private final UserService userService;
    private final TransactionService txnService;
    private final ThemeService themeService;
    private final CallRepository callRepository;
    private final DebtorService debtorService;

    @Override
    @Transactional
    public void authorizeCallStartByTheme(Long clientId, String callId, Long themeId) {
        log.info("=== AUTHORIZE CALL START ===");
        log.info("clientId={}, callId={}, themeId={}", clientId, callId, themeId);

        Theme theme = themeService.findByIdOrThrow(themeId);
        BigDecimal perMinute = ThemePriceUtil.perMinute(theme);
        perMinute = Money.s2(perMinute);

        User client = userService.findByIdForUpdateOrThrow(clientId);

        log.info("User balance: total={}, reserved={}, available={}, required={}",
                client.getBalance(), client.getReserved(), client.getAvailable(), perMinute);

        if (client.getAvailable().compareTo(perMinute) < 0) {
            log.warn("Insufficient funds: userId={}, available={}, required={}",
                    clientId, client.getAvailable(), perMinute);
            throw new InsufficientFundsToStartCallException(
                    client.getId(), themeId, callId, perMinute, client.getAvailable()
            );
        }

        BigDecimal newReserved = Money.s2(client.getReserved().add(perMinute));
        client.setReserved(newReserved);

        // Track preauth using enhanced debtor service
        if (debtorService instanceof DebtorServiceImpl enhanced) {
            enhanced.trackPreAuth(callId, clientId, perMinute);
        }

        log.info("Preauth authorized: callId={}, userId={}, amount={}, newReserved={}, newAvailable={}",
                callId, clientId, perMinute, client.getReserved(), client.getAvailable());
    }

    @Override
    @Transactional
    public Long createDeposit(Long userId, String accountHolder, String nameOfBank, BigDecimal wonAmount) {
        log.info("=== CREATE DEPOSIT ===");
        log.info("userId={}, accountHolder={}, bank={}, amount={}",
                userId, accountHolder, nameOfBank, wonAmount);

        if (wonAmount == null) {
            throw new InvalidDepositAmountException(userId, null);
        }

        BigDecimal amt = Money.s2(wonAmount);
        if (amt.signum() <= 0) {
            log.warn("Invalid deposit amount: userId={}, amount={}", userId, wonAmount);
            throw new InvalidDepositAmountException(userId, wonAmount);
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

        Deposit saved = depositRepo.save(dep);
        log.info("Deposit created: depositId={}, userId={}, amount={}, status={}",
                saved.getId(), userId, amt, saved.getStatus());

        return saved.getId();
    }

    @Override
    @Transactional
    public void confirmDeposit(Long depositId) {
        log.info("=== CONFIRM DEPOSIT ===");
        log.info("depositId={}", depositId);

        Deposit dep = depositRepo.findByIdForUpdate(depositId)
                .orElseThrow(() -> new DepositNotFoundException(depositId));

        if (dep.getStatus() == EStatus.SUCCESSFUL) {
            log.info("Deposit already confirmed: depositId={}, skipping", depositId);
            return;
        }

        BigDecimal original = dep.getWonDecimal();
        if (original == null) {
            throw new InvalidDepositAmountException(dep.getUser().getId(), null);
        }

        BigDecimal amt = Money.s2(original);
        if (amt.signum() <= 0) {
            throw new InvalidDepositAmountException(dep.getUser().getId(), original);
        }

        User user = userService.findByIdForUpdateOrThrow(dep.getUser().getId());
        BigDecimal before = user.getBalance();

        // Repay debts first
        BigDecimal appliedToDebt = debtorService.repayDebt(user, amt);
        BigDecimal remainder = Money.s2(amt.subtract(appliedToDebt));

        // Add remainder to balance
        if (remainder.signum() > 0) {
            user.setBalance(Money.s2(before.add(remainder)));
        }

        log.info("Deposit processed: depositId={}, userId={}, total={}, toDebt={}, toBalance={}, newBalance={}",
                depositId, user.getId(), amt, appliedToDebt, remainder, user.getBalance());

        // Log transaction
        String corr = "deposit:" + dep.getId();
        try {
            txnService.log(
                    user,
                    TransactionType.DEPOSIT,
                    amt,
                    before,
                    user.getBalance(),
                    corr,
                    appliedToDebt.signum() > 0
                            ? String.format("Deposit confirmed (debt repaid: %s, added to balance: %s)",
                            appliedToDebt, remainder)
                            : "Deposit confirmed",
                    EStatus.SUCCESSFUL
            );
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Duplicate transaction detected: depositId={}, corrId={}", depositId, corr);
        }

        dep.setStatus(EStatus.SUCCESSFUL);
        log.info("Deposit confirmed successfully: depositId={}", depositId);
    }

    @Override
    @Transactional
    public void chargeCallAndPay(Long clientId, Long interpreterId, String callId, BigDecimal wonAmount) {
        log.info("=== CHARGE CALL AND PAY ===");
        log.info("clientId={}, interpreterId={}, callId={}, amount={}",
                clientId, interpreterId, callId, wonAmount);

        // Check for duplicate
        if (txnService.existsByCorrelationId(callId)) {
            log.warn("Duplicate call charge detected: callId={}, skipping", callId);
            return;
        }

        if (wonAmount == null) {
            throw new InvalidCallAmountException(clientId, interpreterId, callId, null);
        }

        BigDecimal amt = Money.s2(wonAmount);
        if (amt.signum() <= 0) {
            log.warn("Invalid call amount: callId={}, amount={}", callId, wonAmount);
            throw new InvalidCallAmountException(clientId, interpreterId, callId, wonAmount);
        }

        // Get call theme for logging
        Theme theme = null;
        try {
            Long id = Long.valueOf(callId);
            var call = callRepository.findById(id).orElse(null);
            if (call != null && call.getTheme() != null) {
                theme = call.getTheme();
            }
        } catch (NumberFormatException ignore) {}

        // Lock users in deterministic order to avoid deadlocks
        Long firstId = clientId < interpreterId ? clientId : interpreterId;
        Long secondId = clientId < interpreterId ? interpreterId : clientId;

        User first = userService.findByIdForUpdateOrThrow(firstId);
        User second = userService.findByIdForUpdateOrThrow(secondId);

        User client = first.getId().equals(clientId) ? first : second;
        User interp = first.getId().equals(interpreterId) ? first : second;

        // Release preauth
        if (theme != null) {
            BigDecimal perMinute = ThemePriceUtil.perMinute(theme);
            BigDecimal release = client.getReserved().min(perMinute);
            if (release.signum() > 0) {
                client.setReserved(Money.s2(client.getReserved().subtract(release)));
                log.info("Preauth released: callId={}, userId={}, amount={}, newReserved={}",
                        callId, client.getId(), release, client.getReserved());
            }
        }

        // Debit client
        BigDecimal cBefore = client.getBalance();
        client.setBalance(Money.s2(cBefore.subtract(amt)));

        txnService.log(
                client,
                TransactionType.CALL_DEBIT,
                amt,
                cBefore,
                client.getBalance(),
                callId,
                theme != null ?
                        String.format("Call charge (theme: %s)", theme.getName()) :
                        "Call charge",
                EStatus.SUCCESSFUL
        );

        log.info("Client charged: callId={}, userId={}, amount={}, balanceBefore={}, balanceAfter={}",
                callId, client.getId(), amt, cBefore, client.getBalance());

        // Credit interpreter
        BigDecimal iBefore = interp.getBalance();
        interp.setBalance(Money.s2(iBefore.add(amt)));

        txnService.log(
                interp,
                TransactionType.CALL_CREDIT,
                amt,
                iBefore,
                interp.getBalance(),
                callId,
                theme != null ?
                        String.format("Call payout (theme: %s)", theme.getName()) :
                        "Call payout",
                EStatus.SUCCESSFUL
        );

        log.info("Interpreter paid: callId={}, userId={}, amount={}, balanceBefore={}, balanceAfter={}",
                callId, interp.getId(), amt, iBefore, interp.getBalance());

        // Track new debt if balance went negative
        BigDecimal prevNeg = cBefore.signum() < 0 ? cBefore.abs() : BigDecimal.ZERO;
        BigDecimal newNeg = client.getBalance().signum() < 0 ?
                client.getBalance().abs() : BigDecimal.ZERO;
        BigDecimal deltaNeg = Money.s2(newNeg.subtract(prevNeg));

        if (deltaNeg.signum() > 0) {
            debtorService.addDebt(client, deltaNeg);
            log.info("Debt added: callId={}, userId={}, amount={}", callId, client.getId(), deltaNeg);
        }

        log.info("Call charged successfully: callId={}, client={}, interpreter={}, amount={}",
                callId, clientId, interpreterId, amt);
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit findByIdOrThrow(Long id) {
        return depositRepo.findById(id)
                .orElseThrow(() -> new DepositNotFoundException(id));
    }
}