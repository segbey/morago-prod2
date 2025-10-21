package com.morago.backend.service.language;

import com.morago.backend.dto.LanguageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LanguageService {
    Page<LanguageDto> listLanguages(Pageable pageable, String q);
    LanguageDto createLanguage(LanguageDto dto);
    LanguageDto updateLanguage(Long id, LanguageDto dto);
    void deleteLanguage(Long id);
}