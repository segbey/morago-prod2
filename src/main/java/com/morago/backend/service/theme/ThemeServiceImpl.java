package com.morago.backend.service.theme;

import com.morago.backend.dto.FileResponse;
import com.morago.backend.dto.ThemeDto;
import com.morago.backend.entity.File;
import com.morago.backend.entity.Theme;
import com.morago.backend.exception.theme.ThemeNotFoundException;
import com.morago.backend.mapper.ThemeMapper;
import com.morago.backend.repository.FileRepository;
import com.morago.backend.repository.ThemeRepository;
import com.morago.backend.service.category.CategoryService;
import com.morago.backend.service.file.FileService;
import com.morago.backend.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final StorageService storageService;
    private final FileRepository fileRepository;

    @Value("${app.storage.public-base-url}")
    private String publicBaseUrl;

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.s3.key-prefix:}")
    private String keyPrefix;

    private String normalize(String s) {
        if (s == null || s.isBlank()) return "";
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String toPublicUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) return null;

        // already a full URL
        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) {
            return keyOrUrl;
        }

        String base = normalize(publicBaseUrl);
        String key  = normalize(keyOrUrl);

        // LOCAL storage → no prefix
        if (!"s3".equalsIgnoreCase(storageType)) {
            return base + "/" + key;
        }

        // S3 storage → include prefix if present
        String prefix = normalize(keyPrefix);

        return prefix.isEmpty()
                ? base + "/" + key
                : base + "/" + prefix + "/" + key;
    }


    private ThemeDto enrich(Theme theme) {
        ThemeDto dto = themeMapper.toDto(theme);

        // categoryId
        dto.setCategoryId(theme.getCategory() != null ? theme.getCategory().getId() : null);

        // iconUrl: mapper gives key/path -> convert to public URL
        dto.setIconUrl(toPublicUrl(dto.getIconUrl()));

        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ThemeDto> listThemes(Pageable pageable, String q) {
        if (q != null && !q.isBlank()) {
            return themeRepository.findByNameContainingIgnoreCase(q.trim(), pageable)
                    .map(this::enrich);
        }
        return themeRepository.findAll(pageable).map(this::enrich);
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

        String publicUrl = null;
        if (icon != null && !icon.isEmpty()) {
            FileResponse savedIcon = fileService.uploadThemeIcon(theme.getId(), icon);
            var fileEntity = fileService.findByIdOrThrow(savedIcon.id());

            theme.setIcon(fileEntity);
            theme = themeRepository.save(theme);
            publicUrl = savedIcon.url();
        }

        ThemeDto out = themeMapper.toDto(theme);
        if (publicUrl != null) out.setIconUrl(publicUrl);
        return out;
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

        String publicUrl = null;
        if (icon != null && !icon.isEmpty()) {

            File oldIcon = theme.getIcon();
            if (oldIcon != null && oldIcon.getPath() != null) {
                storageService.delete(oldIcon.getPath());
                fileRepository.delete(oldIcon);
                theme.setIcon(null);
            }

            FileResponse newIcon = fileService.uploadThemeIcon(theme.getId(), icon);
            File fileEntity = fileService.findByIdOrThrow(newIcon.id());

            theme.setIcon(fileEntity);
            theme = themeRepository.save(theme);
            publicUrl = newIcon.url();
        }

        ThemeDto out = themeMapper.toDto(theme);
        if (publicUrl != null) out.setIconUrl(publicUrl);
        return out;
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
                .map(this::enrich)
                .toList();
    }
}