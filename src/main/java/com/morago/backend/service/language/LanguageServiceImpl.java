package com.morago.backend.service.language;

import com.morago.backend.dto.LanguageDto;
import com.morago.backend.entity.Language;
import com.morago.backend.exception.language.LanguageAlreadyExistsException;
import com.morago.backend.exception.language.LanguageInUseException;
import com.morago.backend.exception.language.LanguageNotFoundException;
import com.morago.backend.mapper.LanguageMapper;
import com.morago.backend.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LanguageServiceImpl implements LanguageService {

    private final LanguageRepository languageRepository;
    private final LanguageMapper languageMapper;

    public Language findByIdOrThrow(Long id) {
        return languageRepository.findById(id)
                .orElseThrow(() -> new LanguageNotFoundException(id));
    }

    private String requireAndNormalizeName(String name) {
        String raw = name == null ? "" : name.trim();
        if (raw.isEmpty()) throw new IllegalArgumentException("Language name is required");
        return raw;
    }

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
    public LanguageDto createLanguage(LanguageDto dto) {
        String name = requireAndNormalizeName(dto.getName());
        if (languageRepository.existsByNameIgnoreCase(name)) {
            throw new LanguageAlreadyExistsException(name);
        }
        var saved = languageRepository.save(Language.builder().name(name).build());
        return languageMapper.toDto(saved);
    }

    @Override
    public LanguageDto updateLanguage(Long id, LanguageDto dto) {
        var entity = findByIdOrThrow(id);

        if (dto.getName() != null) {
            String name = requireAndNormalizeName(dto.getName());
            if (!name.equalsIgnoreCase(entity.getName()) &&
                    languageRepository.existsByNameIgnoreCase(name)) {
                throw new LanguageAlreadyExistsException(name);
            }
            entity.setName(name);
        }

        return languageMapper.toDto(languageRepository.save(entity));
    }

    @Override
    public void deleteLanguage(Long id) {
        var lang = findByIdOrThrow(id);

        if (!lang.getTranslatorProfiles().isEmpty()) {
            throw new LanguageInUseException(id);
        }

        languageRepository.delete(lang);
    }
}