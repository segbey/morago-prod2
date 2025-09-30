package com.morago.backend.service.admin;

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
//    void deleteTranslator(Long id);
    void approveTranslator(Long id);
//    void declineTranslator(Long id);


    Page<CallDto> listCalls(Pageable pageable);


    Page<WithdrawalDto> listWithdrawals(Pageable pageable, String status);
    void decideWithdrawal(Long id, boolean approve, String adminNote);


    Page<TransactionAdminDto> listTransactions(Long userId, Pageable pageable);


//    Object createTheme(String name);
//    Object updateTheme(Long id, String name);
//    void deleteTheme(Long id);


    Object createCategory(String name);
    Object updateCategory(Long id, String name);
    void deleteCategory(Long id);
}
