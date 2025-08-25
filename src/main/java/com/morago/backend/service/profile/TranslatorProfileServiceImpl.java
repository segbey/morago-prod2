package com.morago.backend.service.profile;

import com.morago.backend.dto.TranslatorProfileDto;
import com.morago.backend.entity.Language;
import com.morago.backend.entity.Theme;
import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.entity.User;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.mapper.TranslatorProfileMapper;
import com.morago.backend.repository.LanguageRepository;
import com.morago.backend.repository.ThemeRepository;
import com.morago.backend.repository.TranslatorProfileRepository;
import com.morago.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .isAvailable(dto.getIsAvailable())
                .isOnline(dto.getIsOnline())
                .levelOfKorean(dto.getLevelOfKorean())
                .languages(languages)
                .themes(themes)
                .build();

        return mapper.toDto(profileRepo.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public TranslatorProfileDto getById(Long id) {
        return profileRepo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }

    @Override
    public TranslatorProfileDto update(Long id, TranslatorProfileDto dto) {
        TranslatorProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        profile.setEmail(dto.getEmail());
        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setIsAvailable(dto.getIsAvailable());
        profile.setIsOnline(dto.getIsOnline());
        profile.setLevelOfKorean(dto.getLevelOfKorean());

        if (dto.getLanguageIds() != null) {
            Set<Language> languages = dto.getLanguageIds().stream()
                    .map(lid -> languageRepo.findById(lid)
                            .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + lid)))
                    .collect(Collectors.toSet());
            profile.setLanguages(languages);
        }

        if (dto.getThemeIds() != null) {
            Set<Theme> themes = dto.getThemeIds().stream()
                    .map(tid -> themeRepo.findById(tid)
                            .orElseThrow(() -> new ResourceNotFoundException("Theme not found: " + tid)))
                    .collect(Collectors.toSet());
            profile.setThemes(themes);
        }

        return mapper.toDto(profileRepo.save(profile));
    }

    @Override
    public void delete(Long id) {
        TranslatorProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        profileRepo.delete(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranslatorProfileDto> getAll() {
        return profileRepo.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslatorProfileDto> getAll(Pageable pageable) {
        return profileRepo.findAll(pageable).map(mapper::toDto);
    }
}
