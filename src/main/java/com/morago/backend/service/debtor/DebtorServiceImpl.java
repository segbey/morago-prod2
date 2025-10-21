package com.morago.backend.service.debtor;

import com.morago.backend.config.utils.ThemePriceUtil;
import com.morago.backend.dto.DebtorDto;
import com.morago.backend.entity.Debtor;
import com.morago.backend.entity.User;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.mapper.DebtorMapper;
import com.morago.backend.repository.CallRepository;
import com.morago.backend.repository.DebtorRepository;
import com.morago.backend.service.user.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DebtorServiceImpl implements DebtorService {

    private final DebtorRepository debtorRepository;
    private final DebtorMapper debtorMapper;
    private final CallRepository callRepository;
    private final UserService userService;

    @Transactional
    public void addDebt(User user, BigDecimal delta) {
        if (delta == null || delta.signum() <= 0) return;

        var open = debtorRepository.findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(user.getId());
        Debtor debtor = open.isEmpty()
                ? Debtor.builder()
                .user(user)
                .owedDecimal(BigDecimal.ZERO)
                .isPaid(false)
                .build()
                : open.getFirst();

        BigDecimal cur = debtor.getOwedDecimal() == null ? BigDecimal.ZERO : debtor.getOwedDecimal();
        debtor.setOwedDecimal(cur.add(delta));
        debtorRepository.save(debtor);
    }

    @Transactional
    public BigDecimal repayDebt(User user, BigDecimal payment) {
        if (payment == null || payment.signum() <= 0) return BigDecimal.ZERO;

        var open = debtorRepository.findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(user.getId());
        if (open.isEmpty()) return BigDecimal.ZERO;

        BigDecimal remaining = payment;
        BigDecimal appliedTotal = BigDecimal.ZERO;

        for (Debtor d : open) {
            if (remaining.signum() <= 0) break;

            BigDecimal owe = d.getOwedDecimal() == null ? BigDecimal.ZERO : d.getOwedDecimal();
            if (owe.signum() <= 0) {
                d.setPaid(true);
                continue;
            }

            BigDecimal applied = remaining.min(owe);
            BigDecimal left = owe.subtract(applied);

            d.setOwedDecimal(left);
            d.setPaid(left.signum() == 0);

            appliedTotal = appliedTotal.add(applied);
            remaining = remaining.subtract(applied);
        }

        debtorRepository.saveAll(open);
        return appliedTotal;
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

        var call = callRepository.findById(id).orElse(null);
        if (call == null || call.getCaller() == null || call.getTheme() == null) {
            log.debug("releasePreauthByCall: missing call/caller/theme for callId={}", callId);
            return;
        }

        BigDecimal perMinute = ThemePriceUtil.perMinute(call.getTheme());

        User client = userService.findByIdForUpdateOrThrow(call.getCaller().getId());
        BigDecimal toRelease = client.getReserved().min(perMinute);
        if (toRelease.signum() > 0) {
            client.setReserved(client.getReserved().subtract(toRelease));
            log.info("Preauth released {} for call {} (clientId={})", toRelease, callId, client.getId());
        }
    }

    @Override
    @Transactional
    public DebtorDto create(DebtorDto debtorDto) {
        Debtor debtor = debtorMapper.toEntity(debtorDto);
        return debtorMapper.toDto(debtorRepository.save(debtor));
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
        return debtorMapper.toDto(debtorRepository.save(debtor));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Debtor debtor = findDebtorOrThrow(id);
        debtorRepository.delete(debtor);
    }

    private Debtor findDebtorOrThrow(Long id) {
        return debtorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debtor not found with id: " + id));
    }
}
