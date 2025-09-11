package com.morago.backend.service.profile;

import com.morago.backend.dto.TranslatorProfileDto;
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



}