package com.morago.backend.service.admin;

import com.morago.backend.dto.billing.transaction.TransactionAdminDto;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserDto;
import com.morago.backend.entity.Role;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.Roles;
import com.morago.backend.mapper.TransactionMapper;
import com.morago.backend.mapper.TranslatorProfileMapper;
import com.morago.backend.mapper.UserMapper;
import com.morago.backend.mapper.WithdrawalMapper;
import com.morago.backend.repository.CategoryRepository;
import com.morago.backend.repository.ThemeRepository;
import com.morago.backend.repository.UserRepository;
import com.morago.backend.repository.WithdrawalRepository;
import com.morago.backend.service.call.CallService;
import com.morago.backend.service.deposit.DepositService;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.role.RoleService;
import com.morago.backend.service.transaction.TransactionService;
import com.morago.backend.service.withdrawal.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final TranslatorProfileService translatorProfileService;
    private final TranslatorProfileMapper translatorProfileMapper;
    private final UserMapper userMapper;
    private final CallService callService;
    private final WithdrawalRepository withdrawalRepository;
    private final WithdrawalMapper withdrawalMapper;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final WithdrawalService withdrawalService;
    private final RoleService roleService;
    private final DepositService depositService;
    private final ThemeRepository themeRepository;
    private final CategoryRepository categoryRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

//    @Override
//    @Transactional
//    public UserDto createUser(UserDto dto) {
//        User user = userMapper.toEntity(dto);
//        user.setActive(true);
//        return userMapper.toDto(userRepository.save(user));
//    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) throw new RuntimeException("User not found: " + id);
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void setUserActive(Long userId, boolean active) {
        var user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setActive(active);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslatorProfileDto> listTranslators(Pageable pageable) {
        return translatorProfileService.getAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public TranslatorProfileDto getTranslator(Long id) {
        return translatorProfileService.getById(id);
    }

    @Override
    @Transactional
    public TranslatorProfileDto createTranslator(TranslatorProfileDto dto) {
        return translatorProfileService.create(dto);
    }

//    @Override
//    @Transactional
//    public TranslatorProfileDto updateTranslator(Long id, TranslatorProfileDto dto) {
//        return translatorProfileService.update(id, dto);
//    }
//
//    @Override
//    @Transactional
//    public void deleteTranslator(Long id) {
//        translatorProfileService.delete(id);
//    }

    @Override
    @Transactional
    public void approveTranslator(Long id) {
        var profile = translatorProfileService.getById(id);
        Long userId = profile.getUserId();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Role translatorRole = roleService.getRoleOrThrow(Roles.ROLE_TRANSLATOR);
        user.getRoles().add(translatorRole);
        userRepository.save(user);
    }

//    @Override
//    @Transactional
//    public void declineTranslator(Long id) {
//        var profile = translatorProfileService.getById(id);
//        Long userId = profile.getUserId();
//        var user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
//        user.getRoles().removeIf(r -> r.getName() == Roles.ROLE_TRANSLATOR);
//        userRepository.save(user);
//        translatorProfileService.delete(id);
//    }

    @Override
    @Transactional(readOnly = true)
    public Page<CallDto> listCalls(Pageable pageable) {
        var calls = callService.getAllCalls();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), calls.size());
        java.util.List<CallDto> sub = calls.subList(start, Math.max(start, end));
        return new org.springframework.data.domain.PageImpl<>(sub, pageable, calls.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WithdrawalDto> listWithdrawals(Pageable pageable, String status) {
        if (status == null || status.isBlank()) {
            return withdrawalRepository.findAll(pageable).map(withdrawalMapper::toDto);
        } else {
            return withdrawalRepository.findByStatus(status, pageable).map(withdrawalMapper::toDto);
        }
    }

    @Override
    @Transactional
    public void decideWithdrawal(Long id, boolean approve, String adminNote) {
        withdrawalService.decideWithdrawal(id, approve, adminNote);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionAdminDto> listTransactions(Long userId, Pageable pageable) {
        return transactionService.history(userId, pageable).map(transactionMapper::toAdminDto);
    }

//    @Override
//    @Transactional
//    public Object createTheme(String name) {
//        var theme = new com.morago.backend.entity.Theme();
//        theme.setName(name);
//        return themeRepository.save(theme);
//    }
//
//    @Override
//    @Transactional
//    public Object updateTheme(Long id, String name) {
//        var theme = themeRepository.findById(id).orElseThrow(() -> new RuntimeException("Theme not found: " + id));
//        theme.setName(name);
//        return themeRepository.save(theme);
//    }

//    @Override
//    @Transactional
//    public void deleteTheme(Long id) {
//        if (!themeRepository.existsById(id)) throw new RuntimeException("Theme not found: " + id);
//        themeRepository.deleteById(id);
//    }

    @Override
    @Transactional
    public Object createCategory(String name) {
        var category = new com.morago.backend.entity.Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Object updateCategory(Long id, String name) {
        var category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) throw new RuntimeException("Category not found: " + id);
        categoryRepository.deleteById(id);
    }
}
