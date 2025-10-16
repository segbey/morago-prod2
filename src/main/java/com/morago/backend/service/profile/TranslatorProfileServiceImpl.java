package com.morago.backend.service.profile;

import com.morago.backend.dto.ThemeDto;
import com.morago.backend.dto.billing.withdrawal.CreateWithdrawalRequest;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.entity.Language;
import com.morago.backend.entity.Theme;
import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.entity.User;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.exception.rating.SelfRatingNotAllowedException;
import com.morago.backend.mapper.ThemeMapper;
import com.morago.backend.mapper.TranslatorProfileMapper;
import com.morago.backend.repository.LanguageRepository;
import com.morago.backend.repository.ThemeRepository;
import com.morago.backend.repository.TranslatorProfileRepository;
import com.morago.backend.repository.UserRepository;
import com.morago.backend.service.file.FileService;
import com.morago.backend.service.user.UserService;
import com.morago.backend.service.withdrawal.WithdrawalService;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TranslatorProfileServiceImpl implements TranslatorProfileService {

    private final TranslatorProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final LanguageRepository languageRepo;
    private final ThemeRepository themeRepo;
    private final TranslatorProfileMapper mapper;
    private final ThemeMapper themeMapper;
    private final FileService fileService;
    private final WithdrawalService withdrawalService;
    private final UserService userService;

    private static final Set<String> ALLOWED_SORTS =
            Set.of("id", "ratingAvg", "ratingCount", "createdAt");

    @Override
    public TranslatorProfileDto create(TranslatorProfileDto dto) {
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (profileRepo.existsByUser_Id(user.getId())) {
            throw new IllegalStateException("User already has a TranslatorProfile");
        }

        Set<Language> languages = (dto.getLanguageIds() != null) ?
                dto.getLanguageIds().stream()
                        .map(id -> languageRepo.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + id)))
                        .collect(Collectors.toSet()) :
                Set.of();

        Set<Theme> themes = (dto.getThemeIds() != null) ?
                dto.getThemeIds().stream()
                        .map(id -> themeRepo.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Theme not found: " + id)))
                        .collect(Collectors.toSet()) :
                Set.of();

        TranslatorProfile profile = TranslatorProfile.builder()
                .user(user)
                .email(dto.getEmail())
                .dateOfBirth(dto.getDateOfBirth())
                .isVerified(Boolean.TRUE.equals(dto.getIsVerified()))
                .levelOfKorean(dto.getLevelOfKorean())
                .languages(languages)
                .themes(themes)
                .build();

        return mapper.toDto(profileRepo.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public TranslatorProfileDto getById(Long id) {
        TranslatorProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        TranslatorProfileDto dto = mapper.toDto(profile);
        fileService.findByUserIdAndCategory(profile.getUser().getId(), com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public TranslatorProfileDto getByUserId(Long userId) {
        TranslatorProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found for user: " + userId));
        TranslatorProfileDto dto = mapper.toDto(profile);
        fileService.findByUserIdAndCategory(userId, com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranslatorProfileDto> getAll() {
        return profileRepo.findAll().stream()
                .map(profile -> {
                    TranslatorProfileDto dto = mapper.toDto(profile);
                    fileService.findByUserIdAndCategory(profile.getUser().getId(), com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                            .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslatorProfileDto> getAll(Pageable pageable) {
        return profileRepo.findAll(pageable).map(profile -> {
            TranslatorProfileDto dto = mapper.toDto(profile);
            fileService.findByUserIdAndCategory(profile.getUser().getId(), com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                    .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranslatorProfileDto> getOnlineTranslators() {
        return profileRepo.findByIsOnlineTrue().stream()
                .map(profile -> {
                    TranslatorProfileDto dto = mapper.toDto(profile);
                    fileService.findByUserIdAndCategory(profile.getUser().getId(), com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                            .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranslatorProfileDto> getTranslatorsByTheme(Long themeId) {
        return profileRepo.findByThemes_Id(themeId).stream()
                .map(profile -> {
                    TranslatorProfileDto dto = mapper.toDto(profile);
                    fileService.findByUserIdAndCategory(profile.getUser().getId(), com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                            .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranslatorProfileDto> getTranslatorsByLanguage(Long languageId) {
        return profileRepo.findByLanguages_Id(languageId).stream()
                .map(profile -> {
                    TranslatorProfileDto dto = mapper.toDto(profile);
                    fileService.findByUserIdAndCategory(profile.getUser().getId(), com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                            .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
                    return dto;
                })
                .toList();
    }

    @Override
    public void setOnlineStatus(User user, boolean online) {
        profileRepo.findByUser(user).ifPresent(profile -> {
            profile.setIsOnline(online);
            profileRepo.save(profile);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TranslatorProfile getForRatingOrThrow(Long translatorProfileId, Long currentUserId) {
        TranslatorProfile translator = profileRepo.findById(translatorProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Translator not found"));

        if (translator.getUser() != null && translator.getUser().getId().equals(currentUserId)) {
            throw new SelfRatingNotAllowedException();
        }
        return translator;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslatorProfileDto> searchTranslators(
            List<Long> languageIds,
            @Nullable Long themeId,
            @Nullable Boolean online,
            @Nullable Boolean verified,
            Pageable pageable
    ) {
        if (languageIds == null || languageIds.isEmpty()) {
            return Page.empty(pageable);
        }
        long required = languageIds.stream().distinct().count();

        Pageable safe = sanitize(pageable);

        return profileRepo.searchByLanguagesThemeAndFlags(
                        languageIds, themeId, online, verified, required, safe)
                .map(profile -> {
                    TranslatorProfileDto dto = mapper.toDto(profile);
                    fileService.findByUserIdAndCategory(profile.getUser().getId(), com.morago.backend.entity.enumFiles.FileCategory.AVATAR)
                            .ifPresent(file -> dto.setAvatarUrl(file.getPath()));
                    return dto;
                });
    }

    private Pageable sanitize(Pageable p) {
        Sort safeSort = (p.getSort() == null || p.getSort().isUnsorted())
                ? Sort.by(Sort.Order.desc("ratingAvg"), Sort.Order.desc("ratingCount"))
                : Sort.by(
                p.getSort().stream()
                        .filter(o -> ALLOWED_SORTS.contains(o.getProperty()))
                        .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                        .toList()
        );

        if (safeSort.isUnsorted()) {
            safeSort = Sort.by(Sort.Order.desc("ratingAvg"), Sort.Order.desc("ratingCount"));
        }
        return PageRequest.of(p.getPageNumber(), p.getPageSize(), safeSort);
    }

    @Override
    public void updateTranslatorStatus(Long userId, Boolean isOnline) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        TranslatorProfile profile = profileRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found for user: " + userId));

        profile.setIsOnline(isOnline);
        profileRepo.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThemeDto> getMyThemes(Long userId) {
        TranslatorProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found for user: " + userId));
        return profile.getThemes().stream()
                .map(themeMapper::toDto)
                .toList();
    }

    @Override
    public void updateMyThemes(Long userId, Set<Long> themeIds) {
        TranslatorProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found for user: " + userId));

        Set<Theme> newThemes = themeIds.stream()
                .map(id -> themeRepo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Theme not found with ID: " + id)))
                .collect(Collectors.toSet());

        profile.setThemes(newThemes);
        profileRepo.save(profile);
    }

    @Override
    public String updateMyAvatar(Long userId, MultipartFile avatarFile) {
        return fileService.uploadAvatar(userId, avatarFile).getUrl();
    }

    @Override
    public Long requestTranslatorWithdrawal(Long userId, CreateWithdrawalRequest dto) {
        return withdrawalService.requestWithdrawal(
                userId,
                dto.accountNumber(),
                dto.accountHolder(),
                dto.nameOfBank(),
                dto.wonAmount()
        );
    }

    @Override
    public void changeStatus(boolean isOnline) {
        User currentUser = userService.getCurrentUser();
        TranslatorProfile profile = profileRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found for current user"));
        profile.setIsOnline(isOnline);
        profileRepo.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThemeDto> getAvailableThemes() {
        return themeRepo.findAll().stream()
                .map(themeMapper::toDto)
                .toList();
    }

    @Override
    public void updateMyThemes(List<Long> themeIds) {
        User currentUser = userService.getCurrentUser();
        TranslatorProfile profile = profileRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Translator profile not found for current user"));
        Set<Theme> newThemes = themeIds.stream()
                .map(id -> themeRepo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Theme not found with ID: " + id)))
                .collect(Collectors.toSet());
        profile.setThemes(newThemes);
        profileRepo.save(profile);
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        throw new UnsupportedOperationException("Updating avatar by URL is not supported. Please use the file upload endpoint.");
    }
}