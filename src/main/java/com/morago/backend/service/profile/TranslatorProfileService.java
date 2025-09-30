package com.morago.backend.service.profile;

import com.morago.backend.dto.TranslatorProfileDto;
import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.entity.User;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface TranslatorProfileService {

    TranslatorProfileDto create(TranslatorProfileDto dto);

    TranslatorProfileDto getById(Long id);

    TranslatorProfileDto getByUserId(Long userId);

    TranslatorProfileDto update(Long id, TranslatorProfileDto dto);

    void delete(Long id);
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
}