package com.morago.backend.service.theme;

import com.morago.backend.dto.FileResponse;
import com.morago.backend.dto.ThemeDto;
import com.morago.backend.entity.File;
import com.morago.backend.entity.Theme;
import com.morago.backend.exception.theme.ThemeNotFoundException;
import com.morago.backend.mapper.ThemeMapper;
import com.morago.backend.repository.ThemeRepository;
import com.morago.backend.service.category.CategoryService;
import com.morago.backend.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ThemeServiceImpl implements ThemeService {

    private final ThemeRepository themeRepository;
    private final ThemeMapper themeMapper;
    private final FileService fileService;
    private final CategoryService categoryService;

    @Override
    @Transactional(readOnly = true)
    public Page<ThemeDto> listThemes(Pageable pageable, String q) {
        if (q != null && !q.isBlank()) {
            return themeRepository.findByNameContainingIgnoreCase(q.trim(), pageable)
                    .map(themeMapper::toDto);
        }
        return themeRepository.findAll(pageable).map(themeMapper::toDto);
    }

    @Override
    @Transactional
    public ThemeDto createTheme(ThemeDto dto, MultipartFile icon) {
        Theme theme = Theme.builder()
                .name(dto.getName())
                .koreanTitle(dto.getKoreanTitle())
                .price(dto.getPrice())
                .nightPrice(dto.getNightPrice())
                .description(dto.getDescription())
                .isPopular(Boolean.TRUE.equals(dto.getPopular()))
                .isActive(dto.getActive() == null || dto.getActive())
                .build();

        if (dto.getCategoryId() != null) {
            var category = categoryService.findByIdOrThrow(dto.getCategoryId());
            theme.setCategory(category);
        }

        theme = themeRepository.save(theme);

        if (icon != null && !icon.isEmpty()) {
            FileResponse savedIcon = fileService.uploadThemeIcon(theme.getId(), icon);
            var fileEntity = fileService.findByIdOrThrow(savedIcon.id());

            theme.setIcon(fileEntity);
            theme = themeRepository.save(theme);
        }

        return themeMapper.toDto(theme);
    }

    @Override
    @Transactional
    public ThemeDto updateTheme(Long id, ThemeDto dto, MultipartFile icon) {
        Theme theme = findByIdOrThrow(id);

        if (dto.getName() != null) theme.setName(dto.getName());
        if (dto.getKoreanTitle() != null) theme.setKoreanTitle(dto.getKoreanTitle());
        if (dto.getPrice() != null) theme.setPrice(dto.getPrice());
        if (dto.getNightPrice() != null) theme.setNightPrice(dto.getNightPrice());
        if (dto.getDescription() != null) theme.setDescription(dto.getDescription());
        if (dto.getPopular() != null) theme.setPopular(dto.getPopular());
        if (dto.getActive() != null) theme.setActive(dto.getActive());

        if (dto.getCategoryId() != null) {
            var category = categoryService.findByIdOrThrow(dto.getCategoryId());
            theme.setCategory(category);
        }

        theme = themeRepository.save(theme);

        if (icon != null && !icon.isEmpty()) {
            FileResponse newIcon = fileService.uploadThemeIcon(theme.getId(), icon);
            File fileEntity = fileService.findByIdOrThrow(newIcon.id());

            theme.setIcon(fileEntity);
            theme = themeRepository.save(theme);
        }

        return themeMapper.toDto(theme);
    }

    @Override
    @Transactional
    public void deleteTheme(Long id) {
        Theme theme = findByIdOrThrow(id);
        themeRepository.delete(theme);
    }

    @Override
    @Transactional(readOnly = true)
    public Theme findByIdOrThrow(Long id) {
        return themeRepository.findById(id)
                .orElseThrow(() -> new ThemeNotFoundException(id));
    }


    @Override
    @Transactional(readOnly = true)
    public List<ThemeDto> getActiveThemes() {
        return themeRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(themeMapper::toDto)
                .toList();
    }
}