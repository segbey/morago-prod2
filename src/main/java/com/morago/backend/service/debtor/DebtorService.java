package com.morago.backend.service.debtor;

import com.morago.backend.dto.DebtorDto;
import com.morago.backend.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface DebtorService {
    void addDebt(User user, BigDecimal delta);
    BigDecimal repayDebt(User user, BigDecimal payment);
    void releasePreauthByCall(String callId);

    DebtorDto create(DebtorDto debtorDto);
    DebtorDto getById(Long id);
    List<DebtorDto> getAll();
    DebtorDto update(Long id, DebtorDto debtorDto);
    void delete(Long id);
}
