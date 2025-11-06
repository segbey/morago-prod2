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
public class DepositServiceImpl implements DepositService{
    private final DepositRepository depositRepo;
    private final UserService userService;
    private final TransactionService txnService;
    private final ThemeService themeService;
    private final CallRepository callRepository;
    private final DebtorService debtorService;

    @Override
    @Transactional
    public void authorizeCallStartByTheme(Long clientId, String callId, Long themeId) {
        Theme theme = themeService.findByIdOrThrow(themeId);

        BigDecimal perMinute = ThemePriceUtil.perMinute(theme);

        User client = userService.findByIdForUpdateOrThrow(clientId);

        if (client.getAvailable().compareTo(perMinute) < 0) {
            throw new InsufficientFundsToStartCallException(
                    client.getId(), themeId, callId, perMinute, client.getAvailable()
            );
        }

        client.setReserved(client.getReserved().add(perMinute));

        log.info("Preauth reserved {} for call {} (clientId={}, themeId={})",
                perMinute, callId, clientId, themeId);
    }

    @Override
    @Transactional
    public Long createDeposit(Long userId, String accountHolder, String nameOfBank, BigDecimal wonAmount) {
        if (wonAmount == null) {
            throw new InvalidDepositAmountException(userId, null);
        }
        BigDecimal amt = Money.s2(wonAmount);
        if (amt.signum() <= 0) {
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

        return depositRepo.save(dep).getId();
    }

    @Override
    @Transactional
    public void confirmDeposit(Long depositId) {
        Deposit dep = depositRepo.findByIdForUpdate(depositId)
                .orElseThrow(() -> new DepositNotFoundException(depositId));

        if (dep.getStatus() == EStatus.SUCCESSFUL) {
            log.info("[DEPOSIT] Deposit {} already confirmed, skipping", depositId);
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

        BigDecimal appliedToDebt = debtorService.repayDebt(user, amt);
        BigDecimal remainder = amt.subtract(appliedToDebt);

        BigDecimal before = user.getBalance();
        if (remainder.signum() > 0) {
            user.setBalance(before.add(remainder));
        }

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
                            ? "Deposit confirmed (part used to repay debt: " + appliedToDebt + ")"
                            : "Deposit confirmed",
                    EStatus.SUCCESSFUL
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("[DEPOSIT] Duplicate transaction for depositId={}, corrId={}", depositId, corr);
        }

        dep.setStatus(EStatus.SUCCESSFUL);
        log.info("[DEPOSIT] Confirmed depositId={} userId={} amount={} appliedToDebt={} remainder={} newBalance={}",
                depositId, user.getId(), amt, appliedToDebt, remainder, user.getBalance());
    }

    @Override
    @Transactional
    public void chargeCallAndPay(Long clientId, Long interpreterId, String callId, BigDecimal wonAmount) {
        if (txnService.existsByCorrelationId(callId)) {
            log.warn("[CALL] Duplicate call charge detected, callId={}", callId);
            return;
        }
        if (wonAmount == null) {
            throw new InvalidCallAmountException(clientId, interpreterId, callId, null);
        }
        BigDecimal amt = Money.s2(wonAmount);
        if (amt.signum() <= 0) {
            throw new InvalidCallAmountException(clientId, interpreterId, callId, wonAmount);
        }

        Theme theme = null;
        try {
            Long id = Long.valueOf(callId);
            var call = callRepository.findById(id).orElse(null);
            if (call != null && call.getTheme() != null) {
                theme = call.getTheme();
            }
        } catch (NumberFormatException ignore) {

        }

        Long firstId = clientId < interpreterId ? clientId : interpreterId;
        Long secondId = clientId < interpreterId ? interpreterId : clientId;

        User first  = userService.findByIdForUpdateOrThrow(firstId);
        User second = userService.findByIdForUpdateOrThrow(secondId);

        User client = first.getId().equals(clientId) ? first : second;
        User interp = first.getId().equals(interpreterId) ? first : second;

        if (theme != null) {
            BigDecimal perMinute = ThemePriceUtil.perMinute(theme);
            BigDecimal release = client.getReserved().min(perMinute);
            if (release.signum() > 0) {
                client.setReserved(client.getReserved().subtract(release));
                log.info("Preauth released {} for call {} (clientId={}, themeId={})",
                        release, callId, client.getId(), theme.getId());
            }
        }

        BigDecimal cBefore = client.getBalance();
        client.setBalance(cBefore.subtract(amt));
        txnService.log(client, TransactionType.CALL_DEBIT, amt, cBefore, client.getBalance(),
                callId, "Call charge", EStatus.SUCCESSFUL);

        BigDecimal iBefore = interp.getBalance();
        interp.setBalance(iBefore.add(amt));
        txnService.log(interp, TransactionType.CALL_CREDIT, amt, iBefore, interp.getBalance(),
                callId, "Call payout", EStatus.SUCCESSFUL);

        BigDecimal prevNeg = cBefore.signum() < 0 ? cBefore.abs() : BigDecimal.ZERO;
        BigDecimal newNeg  = client.getBalance().signum() < 0 ? client.getBalance().abs() : BigDecimal.ZERO;
        BigDecimal deltaNeg = newNeg.subtract(prevNeg);

        if (deltaNeg.signum() > 0 && debtorService != null) {
            debtorService.addDebt(client, deltaNeg);
        }
        log.info("[CALL] Charged clientId={} interpId={} callId={} amount={} clientNewBalance={} interpNewBalance={}",
                clientId, interpreterId, callId, amt, client.getBalance(), interp.getBalance());
    }


    @Override
    @Transactional(readOnly = true)
    public Deposit findByIdOrThrow(Long id) {
        return depositRepo.findById(id)
                .orElseThrow(() -> new DepositNotFoundException(id));
    }
}