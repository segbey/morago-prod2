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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtorServiceImpl implements DebtorService {

    private final DebtorRepository debtorRepository;
    private final DebtorMapper debtorMapper;
    private final CallRepository callRepository;
    private final UserService userService;

    @Transactional
    public void addDebt(User user, BigDecimal delta) {
        if (delta == null || delta.signum() <= 0) return;

        List<Debtor> open = debtorRepository.findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(user.getId());
        Debtor debtor = open.isEmpty()
                ? Debtor.builder()
                .user(user)
                .owedDecimal(BigDecimal.ZERO)
                .isPaid(false)
                .build()
                : open.get(0);

        BigDecimal cur = debtor.getOwedDecimal() == null ? BigDecimal.ZERO : debtor.getOwedDecimal();
        debtor.setOwedDecimal(cur.add(delta));
        debtorRepository.save(debtor);
    }

    @Transactional
    public BigDecimal repayDebt(User user, BigDecimal payment) {
        if (payment == null || payment.signum() <= 0) return BigDecimal.ZERO;

        List<Debtor> open = debtorRepository.findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(user.getId());
        if (open.isEmpty()) return BigDecimal.ZERO;

        BigDecimal remaining = payment;
        BigDecimal appliedTotal = BigDecimal.ZERO;

        for (Debtor d : open) {
            if (remaining.signum() <= 0) break;

            BigDecimal owe = d.getOwedDecimal() == null ? BigDecimal.ZERO : d.getOwedDecimal();
            if (owe.signum() <= 0) {
                d.setPaid(true);
                debtorRepository.save(d);
                continue;
            }

            BigDecimal applied = remaining.min(owe);
            BigDecimal left = owe.subtract(applied);

            d.setOwedDecimal(left);
            d.setPaid(left.signum() == 0);
            debtorRepository.save(d);

            appliedTotal = appliedTotal.add(applied);
            remaining = remaining.subtract(applied);
        }

        return appliedTotal;
    }

    @Override
    @Transactional
    public void releasePreauthByCall(String callId) {
        Long id;
        try {
            id = Long.valueOf(callId);
        } catch (NumberFormatException e) {
            return;
        }

        var call = callRepository.findById(id).orElse(null);
        if (call == null || call.getCaller() == null || call.getTheme() == null) return;

        BigDecimal perMinute = ThemePriceUtil.perMinute(call.getTheme());
        if (perMinute.signum() <= 0) return;

        User client = userService.findByIdForUpdateOrThrow(call.getCaller().getId());

        BigDecimal toRelease = client.getReserved().min(perMinute);
        if (toRelease.signum() > 0) {
            client.setReserved(client.getReserved().subtract(toRelease));
            log.info("Preauth released {} for call {} (clientId={})", toRelease, callId, client.getId());
        }
    }

    @Override
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
                .collect(Collectors.toList());
    }

    @Override
    public DebtorDto update(Long id, DebtorDto debtorDto) {
        Debtor debtor = findDebtorOrThrow(id);

        debtor.setAccountHolder(debtorDto.getAccountHolder());
        debtor.setNameOfBank(debtorDto.getNameOfBank());
        debtor.setPaid(debtorDto.isPaid());

        return debtorMapper.toDto(debtorRepository.save(debtor));
    }

    @Override
    public void delete(Long id) {
        Debtor debtor = findDebtorOrThrow(id);
        debtorRepository.delete(debtor);
    }

    private Debtor findDebtorOrThrow(Long id) {
        return debtorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debtor not found with id: " + id));
    }
}
