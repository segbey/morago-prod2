package com.morago.backend.service.admin;

import com.morago.backend.dto.admin.AdminTranslatorDto;
import com.morago.backend.dto.admin.AdminUserDto;
import com.morago.backend.dto.billing.transaction.TransactionAdminDto;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AdminService {


    Page<UserDto> listUsers(Pageable pageable);
    UserDto createUser(AdminUserDto dto);
    //    UserDto updateUser(Long id, UserDto req);
    void deleteUser(Long id);
    void setUserActive(Long userId, boolean active);


    Page<TranslatorProfileDto> listTranslators(Pageable pageable);
    TranslatorProfileDto getTranslator(Long id);
    TranslatorProfileDto createTranslator(AdminTranslatorDto req,
                                          MultipartFile avatar,
                                          MultipartFile[] docs);
    //    TranslatorProfileDto updateTranslator(Long id, TranslatorProfileDto dto);
    void deleteTranslator(Long id);
    void approveTranslator(Long id);
    void declineTranslator(Long id);


    Page<CallDto> listCalls(Pageable pageable);


    Page<WithdrawalDto> listWithdrawals(Pageable pageable, String status);
    void decideWithdrawal(Long id, boolean approve, String adminNote);


    Page<TransactionAdminDto> listTransactions(Long userId, Pageable pageable);


    Page<CallDto> listCallsByUser(Long userId, Pageable pageable);
    Page<CallDto> listCallsByTranslator(Long translatorUserId, Pageable pageable);

    // Withdrawals (translator == their user id)
    Page<WithdrawalDto> listWithdrawalsByTranslator(Long translatorUserId, Pageable pageable, String status);

    // Deposits (per user)
    Page<TransactionAdminDto> listDepositsByUser(Long userId, Pageable pageable);

}
