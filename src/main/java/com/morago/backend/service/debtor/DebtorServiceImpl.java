package com.morago.backend.service.debtor;

import com.morago.backend.config.utils.ThemePriceUtil;
import com.morago.backend.dto.DebtorDto;
import com.morago.backend.entity.Debtor;
import com.morago.backend.entity.Money;
import com.morago.backend.entity.User;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.mapper.DebtorMapper;
import com.morago.backend.repository.CallRepository;
import com.morago.backend.repository.DebtorRepository;
import com.morago.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DebtorServiceImpl implements DebtorService {

    private final DebtorRepository debtorRepository;
    private final DebtorMapper debtorMapper;
    private final CallRepository callRepository;
    private final UserService userService;

    private final Map<String, PreAuthData> preAuthTracker = new ConcurrentHashMap<>();

    private static class PreAuthData {
        Long userId;
        BigDecimal amount;
        java.time.LocalDateTime createdAt;

        PreAuthData(Long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
            this.createdAt = java.time.LocalDateTime.now();
        }
    }

    public void trackPreAuth(String callId, Long userId, BigDecimal amount) {
        preAuthTracker.put(callId, new PreAuthData(userId, amount));
        log.info("Tracked preauth: callId={}, userId={}, amount={}", callId, userId, amount);
    }

    public BigDecimal getTrackedPreAuth(String callId) {
        PreAuthData data = preAuthTracker.get(callId);
        return data != null ? data.amount : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void addDebt(User user, BigDecimal delta) {
        if (delta == null || delta.signum() <= 0) {
            log.debug("Skipping addDebt: invalid delta={}", delta);
            return;
        }

        delta = Money.s2(delta);

        var openDebts = debtorRepository.findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(user.getId());
        Debtor debtor = openDebts.isEmpty()
                ? Debtor.builder()
                .user(user)
                .owedDecimal(BigDecimal.ZERO)
                .isPaid(false)
                .build()
                : openDebts.getFirst();

        BigDecimal current = debtor.getOwedDecimal() == null ?
                BigDecimal.ZERO : debtor.getOwedDecimal();
        BigDecimal newTotal = Money.s2(current.add(delta));

        debtor.setOwedDecimal(newTotal);
        debtorRepository.save(debtor);

        log.info("Added debt: userId={}, delta={}, previousDebt={}, newDebt={}",
                user.getId(), delta, current, newTotal);
    }

    @Override
    @Transactional
    public BigDecimal repayDebt(User user, BigDecimal payment) {
        if (payment == null || payment.signum() <= 0) {
            log.debug("Skipping repayDebt: invalid payment={}", payment);
            return BigDecimal.ZERO;
        }

        payment = Money.s2(payment);

        var openDebts = debtorRepository.findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(user.getId());
        if (openDebts.isEmpty()) {
            log.debug("No open debts for userId={}", user.getId());
            return BigDecimal.ZERO;
        }

        BigDecimal remaining = payment;
        BigDecimal totalApplied = BigDecimal.ZERO;

        for (Debtor debtor : openDebts) {
            if (remaining.signum() <= 0) break;

            BigDecimal owed = debtor.getOwedDecimal() == null ?
                    BigDecimal.ZERO : debtor.getOwedDecimal();

            if (owed.signum() <= 0) {
                debtor.setPaid(true);
                continue;
            }

            BigDecimal applied = remaining.min(owed);
            BigDecimal leftOver = Money.s2(owed.subtract(applied));

            debtor.setOwedDecimal(leftOver);
            debtor.setPaid(leftOver.signum() == 0);

            totalApplied = totalApplied.add(applied);
            remaining = remaining.subtract(applied);

            log.info("Repaid debt: debtorId={}, userId={}, owed={}, applied={}, remaining={}, paid={}",
                    debtor.getId(), user.getId(), owed, applied, leftOver, debtor.isPaid());
        }

        debtorRepository.saveAll(openDebts);

        log.info("Total debt repayment: userId={}, payment={}, applied={}, leftover={}",
                user.getId(), payment, totalApplied, remaining);

        return totalApplied;
    }

    @Override
    @Transactional
    public void releasePreauthByCall(String callId) {
        final long id;
        try {
            id = Long.parseLong(callId);
        } catch (NumberFormatException e) {
            log.debug("releasePreauthByCall: non-numeric callId={}", callId);
            return;
        }

        PreAuthData tracked = preAuthTracker.remove(callId);
        if (tracked != null) {
            log.info("Found tracked preauth for callId={}, userId={}, amount={}",
                    callId, tracked.userId, tracked.amount);

            try {
                User user = userService.findByIdForUpdateOrThrow(tracked.userId);
                BigDecimal toRelease = user.getReserved().min(tracked.amount);

                if (toRelease.signum() > 0) {
                    user.setReserved(Money.s2(user.getReserved().subtract(toRelease)));
                    log.info("Released tracked preauth: callId={}, userId={}, amount={}, newReserved={}",
                            callId, tracked.userId, toRelease, user.getReserved());
                }
            } catch (Exception e) {
                log.error("Failed to release tracked preauth for callId={}: {}", callId, e.getMessage(), e);
            }
            return;
        }

        var call = callRepository.findById(id).orElse(null);
        if (call == null || call.getCaller() == null || call.getTheme() == null) {
            log.debug("releasePreauthByCall: missing call/caller/theme for callId={}", callId);
            return;
        }

        try {
            BigDecimal perMinute = ThemePriceUtil.perMinute(call.getTheme());
            User client = userService.findByIdForUpdateOrThrow(call.getCaller().getId());
            BigDecimal toRelease = client.getReserved().min(perMinute);

            if (toRelease.signum() > 0) {
                client.setReserved(Money.s2(client.getReserved().subtract(toRelease)));
                log.info("Released preauth (fallback): callId={}, userId={}, amount={}, newReserved={}",
                        callId, client.getId(), toRelease, client.getReserved());
            }
        } catch (Exception e) {
            log.error("Failed to release preauth for callId={}: {}", callId, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalDebt(Long userId) {
        var debts = debtorRepository.findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(userId);
        BigDecimal total = debts.stream()
                .map(d -> d.getOwedDecimal() == null ? BigDecimal.ZERO : d.getOwedDecimal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Money.s2(total);
    }

    @Transactional(readOnly = true)
    public boolean hasDebt(Long userId) {
        return debtorRepository.existsByUserIdAndIsPaidFalse(userId);
    }

    @Override
    @Transactional
    public DebtorDto create(DebtorDto debtorDto) {
        Debtor debtor = debtorMapper.toEntity(debtorDto);
        Debtor saved = debtorRepository.save(debtor);
        log.info("Created debtor: id={}, userId={}", saved.getId(), saved.getUser().getId());
        return debtorMapper.toDto(saved);
    }

    @Override
    public DebtorDto getById(Long id) {
        Debtor debtor = findDebtorOrThrow(id);
        return debtorMapper.toDto(debtor);
    }

    @Override
    public List<DebtorDto> getAll() {
        return debtorRepository.findAll()
                .stream()
                .map(debtorMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DebtorDto update(Long id, DebtorDto debtorDto) {
        Debtor debtor = findDebtorOrThrow(id);
        debtor.setAccountHolder(debtorDto.getAccountHolder());
        debtor.setNameOfBank(debtorDto.getNameOfBank());
        debtor.setPaid(debtorDto.isPaid());

        Debtor updated = debtorRepository.save(debtor);
        log.info("Updated debtor: id={}", id);
        return debtorMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Debtor debtor = findDebtorOrThrow(id);
        debtorRepository.delete(debtor);
        log.info("Deleted debtor: id={}", id);
    }

    private Debtor findDebtorOrThrow(Long id) {
        return debtorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debtor not found with id: " + id));
    }
}
