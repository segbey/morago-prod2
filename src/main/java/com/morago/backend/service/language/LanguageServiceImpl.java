package com.morago.backend.service.language;

import com.morago.backend.dto.LanguageDto;
import com.morago.backend.entity.Language;
import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.exception.ResourceAlreadyExistsException;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.mapper.LanguageMapper;
import com.morago.backend.repository.LanguageRepository;
import com.morago.backend.repository.TranslatorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LanguageServiceImpl implements LanguageService {

    private final LanguageRepository languageRepository;
    private final TranslatorProfileRepository translatorProfileRepository;
    private final LanguageMapper languageMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<LanguageDto> listLanguages(Pageable pageable, String q) {
        Page<Language> page =
                (q != null && !q.isBlank())
                        ? languageRepository.findByNameContainingIgnoreCase(q.trim(), pageable)
                        : languageRepository.findAll(pageable);
        return page.map(languageMapper::toDto);
    }

    @Override
    @Transactional
    public LanguageDto createLanguage(LanguageDto dto) {
        String raw = dto.getName() == null ? "" : dto.getName().trim();
        if (raw.isEmpty()) throw new IllegalArgumentException("Language name is required");
        if (languageRepository.existsByNameIgnoreCase(raw)) {
            throw new ResourceAlreadyExistsException("Language already exists: " + raw);
        }
        var entity = Language.builder().name(raw).build();
        var saved = languageRepository.save(entity);
        return languageMapper.toDto(saved);
    }

    @Override
    @Transactional
    public LanguageDto updateLanguage(Long id, LanguageDto dto) {
        var entity = languageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + id));

        if (dto.getName() != null) {
            String raw = dto.getName().trim();
            if (raw.isEmpty()) throw new IllegalArgumentException("Language name cannot be blank");
            if (!raw.equalsIgnoreCase(entity.getName()) && languageRepository.existsByNameIgnoreCase(raw)) {
                throw new ResourceAlreadyExistsException("Language already exists: " + raw);
            }
            entity.setName(raw);
        }

        var saved = languageRepository.save(entity);
        return languageMapper.toDto(saved);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteLanguage(Long id) {
        var lang = languageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + id));

        if (!lang.getTranslatorProfiles().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Language is in use by one or more translators"
            );
        }

        languageRepository.delete(lang);
    }
}