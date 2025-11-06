package com.morago.backend.service.language;

import com.morago.backend.dto.LanguageDto;
import com.morago.backend.entity.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LanguageService {
    Page<LanguageDto> listLanguages(Pageable pageable, String q);
    LanguageDto createLanguage(LanguageDto dto);
    LanguageDto updateLanguage(Long id, LanguageDto dto);
    void deleteLanguage(Long id);
    Language findByIdOrThrow(Long id);
}