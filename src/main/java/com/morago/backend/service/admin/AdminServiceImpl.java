package com.morago.backend.service.admin;

import com.morago.backend.dto.admin.AdminTranslatorDto;
import com.morago.backend.dto.admin.AdminUserDto;
import com.morago.backend.dto.billing.transaction.TransactionAdminDto;
import com.morago.backend.dto.billing.withdrawal.WithdrawalDto;
import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserDto;
import com.morago.backend.entity.*;
import com.morago.backend.entity.enumFiles.FileCategory;
import com.morago.backend.entity.enumFiles.Roles;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.exception.phonenumber.PhoneAlreadyExistsException;
import com.morago.backend.mapper.*;
import com.morago.backend.repository.*;
import com.morago.backend.service.call.CallService;
import com.morago.backend.service.deposit.DepositService;
import com.morago.backend.service.file.FileService;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.role.RoleService;
import com.morago.backend.service.transaction.TransactionService;
import com.morago.backend.service.withdrawal.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.morago.backend.entity.enumFiles.TransactionType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

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
    private final FileRepository fileRepository;
    private final CallRepository callRepository;
    private final CallMapper callMapper;
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;
    private final RatingRepository ratingRepository;
    private final TranslatorProfileRepository translatorProfileRepository;
    private final FileService fileService;
    private final LanguageRepository languageRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional
    public UserDto createUser(AdminUserDto req) {

        if (userRepository.existsByUsername(req.getPhoneNumber())) {
            throw new PhoneAlreadyExistsException(req.getPhoneNumber());
        }

        BigDecimal initialBalance = req.getBalance() != null ? req.getBalance() : BigDecimal.ZERO;

        User user = new User();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUsername(req.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setBalance(initialBalance);
        user.setRoles(new HashSet<>());

        Role defaultRole = roleService.getRoleOrThrow(Roles.ROLE_USER);
        user.getRoles().add(defaultRole);

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

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
    @Transactional(rollbackFor = Exception.class)
    public TranslatorProfileDto createTranslator(AdminTranslatorDto req,
                                                 MultipartFile avatar,
                                                 MultipartFile[] docs) {

        if (userRepository.existsByUsername(req.getPhoneNumber())) {
            throw new PhoneAlreadyExistsException(req.getPhoneNumber());
        }

        User user = new User();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUsername(req.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRoles(new HashSet<>());

        user.getRoles().add(roleService.getRoleOrThrow(Roles.ROLE_TRANSLATOR));
        user = userRepository.save(user);

        List<Language> languages = languageRepository.findAllById(req.getLanguageIds());
        if (languages.size() != req.getLanguageIds().size()) {
            throw new ResourceNotFoundException("One or more languages not found");
        }
        List<Theme> themes = themeRepository.findAllById(req.getThemeIds());
        if (themes.size() != req.getThemeIds().size()) {
            throw new ResourceNotFoundException("One or more themes not found");
        }


        TranslatorProfile profile = TranslatorProfile.builder()
                .user(user)
                .dateOfBirth(req.getDateOfBirth())
                .levelOfKorean(req.getLevelOfKorean().toUpperCase())
                .isVerified(false)
                .build();

        profile.setLanguages(new HashSet<>(languages));
        profile.setThemes(new HashSet<>(themes));

        // Upload files (still inside TX; throw to rollback on failure)
        if (avatar != null && !avatar.isEmpty()) {
            fileService.uploadAvatar(user.getId(), avatar);
        }
        if (docs != null) {
            for (MultipartFile doc : docs) {
                if (doc != null && !doc.isEmpty()) {
                    fileService.uploadTranslatorDoc(user.getId(), doc); // must throw on failure
                }
            }
        }

        return translatorProfileMapper.toDto(translatorProfileRepository.save(profile));
    }

    //    @Override
//    @Transactional
//    public TranslatorProfileDto updateTranslator(Long id, TranslatorProfileDto dto) {
//        return translatorProfileService.update(id, dto);
//    }
//
    @Override
    @Transactional
    public void deleteTranslator(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        var profile = user.getTranslatorProfile();

        if (profile == null) {
            user.getRoles().removeIf(r -> r.getName() == Roles.ROLE_TRANSLATOR);
            userRepository.save(user);
            return;
        }

        if (withdrawalRepository.existsByUser_IdAndStatus(userId, "PENDING")) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Translator has a pending withdrawal; resolve it before deletion."
            );
        }

        boolean hasLiveCalls =
                callRepository.existsByCaller_IdAndEndCallFalse(userId) ||
                        callRepository.existsByRecipient_IdAndEndCallFalse(userId);

        if (hasLiveCalls) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Translator has ongoing calls; end them before deletion."
            );
        }

        ratingRepository.deleteByTranslatorId(profile.getId());

        fileRepository.findByUserIdAndCategory(userId, FileCategory.AVATAR)
                .ifPresent(fileRepository::delete);
        fileRepository.findByUserIdAndCategory(userId, FileCategory.ICON)
                .ifPresent(fileRepository::delete);

        if (profile.getThemes() != null) {
            profile.getThemes().clear();
        }

//    translatorProfileService.delete(profile.getId());
        translatorProfileRepository.deleteById(profile.getId());

        user.setTranslatorProfile(null);
        user.getRoles().removeIf(r -> r.getName() == Roles.ROLE_TRANSLATOR);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void approveTranslator(Long id) {
        var profile = translatorProfileService.getById(id);
        Long userId = profile.getUserId();
        TranslatorProfile tprofile = translatorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Translator profile not found: " + id));

        tprofile.setIsVerified(true);
        translatorProfileRepository.save(tprofile);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Role translatorRole = roleService.getRoleOrThrow(Roles.ROLE_TRANSLATOR);
        user.getRoles().add(translatorRole);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void declineTranslator(Long id) {
        var profile = translatorProfileService.getById(id);
        Long userId = profile.getUserId();

        TranslatorProfile dtprofile = translatorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Translator profile not found: " + id));

        dtprofile.setIsVerified(false);
        translatorProfileRepository.save(dtprofile);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.getRoles().removeIf(r -> r.getName() == Roles.ROLE_TRANSLATOR);
        userRepository.save(user);
//        translatorProfileService.delete(id);
    }

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


    @Override
    @Transactional(readOnly = true)
    public Page<CallDto> listCallsByUser(Long userId, Pageable pageable) {
        var page = callRepository.findByCaller_IdOrRecipient_Id(userId, userId, pageable);
        return page.map(callMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CallDto> listCallsByTranslator(Long translatorUserId, Pageable pageable) {
        var page = callRepository.findByCaller_IdOrRecipient_Id(translatorUserId, translatorUserId, pageable);
        return page.map(callMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WithdrawalDto> listWithdrawalsByTranslator(Long translatorUserId, Pageable pageable, String status) {
        if (status == null || status.isBlank()) {
            return withdrawalRepository.findByUser_Id(translatorUserId, pageable)
                    .map(withdrawalMapper::toDto);
        }
        return withdrawalRepository.findByUser_IdAndStatus(translatorUserId, status.trim(), pageable)
                .map(withdrawalMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionAdminDto> listDepositsByUser(Long userId, Pageable pageable) {
        var page = transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                userId, TransactionType.DEPOSIT, pageable);
        return page.map(transactionMapper::toAdminDto);
    }
}
