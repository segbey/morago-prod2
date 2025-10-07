package com.morago.backend.service.admin;

import com.morago.backend.dto.CategoryDto;
import com.morago.backend.dto.ThemeDto;
import com.morago.backend.dto.billing.transaction.TransactionAdminDto;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {


    Page<UserDto> listUsers(Pageable pageable);
//    UserDto createUser(UserDto dto);
//    UserDto updateUser(Long id, UserDto dto);
    void deleteUser(Long id);
    void setUserActive(Long userId, boolean active);


    Page<TranslatorProfileDto> listTranslators(Pageable pageable);
    TranslatorProfileDto getTranslator(Long id);
    TranslatorProfileDto createTranslator(TranslatorProfileDto dto);
    //    TranslatorProfileDto updateTranslator(Long id, TranslatorProfileDto dto);
    void deleteTranslator(Long id);
    void approveTranslator(Long id);
    void declineTranslator(Long id);


    Page<CallDto> listCalls(Pageable pageable);


    Page<WithdrawalDto> listWithdrawals(Pageable pageable, String status);
    void decideWithdrawal(Long id, boolean approve, String adminNote);


    Page<TransactionAdminDto> listTransactions(Long userId, Pageable pageable);

    Page<ThemeDto> listThemes(Pageable pageable, String q);
    ThemeDto createTheme(ThemeDto dto);
    ThemeDto updateTheme(Long id, ThemeDto dto);
    void deleteTheme(Long id);

    Page<CategoryDto> listCategories(Pageable pageable, String q);
    Object createCategory(String name);
    Object updateCategory(Long id, String name);
    void deleteCategory(Long id);

    Page<CallDto> listCallsByUser(Long userId, Pageable pageable);
    Page<CallDto> listCallsByTranslator(Long translatorUserId, Pageable pageable);

    // Withdrawals (translator == their user id)
    Page<WithdrawalDto> listWithdrawalsByTranslator(Long translatorUserId, Pageable pageable, String status);

    // Deposits (per user)
    Page<TransactionAdminDto> listDepositsByUser(Long userId, Pageable pageable);
}
