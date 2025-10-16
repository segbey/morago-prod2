package com.morago.backend.service.profile;

import com.morago.backend.dto.ThemeDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.entity.User;
import org.springframework.lang.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.morago.backend.dto.billing.withdrawal.CreateWithdrawalRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface TranslatorProfileService {

    TranslatorProfileDto create(TranslatorProfileDto dto);

    TranslatorProfileDto getById(Long id);

    TranslatorProfileDto getByUserId(Long userId);

    void changeStatus(boolean isOnline);
    List<ThemeDto> getAvailableThemes();
    void updateMyThemes(List<Long> themeIds);
    void updateAvatar(String avatarUrl);


    List<TranslatorProfileDto> getAll();
    Page<TranslatorProfileDto> getAll(Pageable pageable);

    List<TranslatorProfileDto> getOnlineTranslators();
    List<TranslatorProfileDto> getTranslatorsByTheme(Long themeId);
    List<TranslatorProfileDto> getTranslatorsByLanguage(Long languageId);


    void setOnlineStatus(User user, boolean b);
    TranslatorProfile getForRatingOrThrow(Long translatorProfileId, Long currentUserId);

    Page<TranslatorProfileDto> searchTranslators(
            List<Long> languageIds,
            @Nullable Long themeId,
            @Nullable Boolean online,
            @Nullable Boolean verified,
            Pageable pageable
    );

    void updateTranslatorStatus(Long userId, Boolean isOnline);
    List<ThemeDto> getMyThemes(Long userId);
    void updateMyThemes(Long userId, Set<Long> themeIds);
    String updateMyAvatar(Long userId, MultipartFile avatarFile);
    Long requestTranslatorWithdrawal(Long userId, CreateWithdrawalRequest dto);

}