package com.morago.backend.service.theme;

import com.morago.backend.dto.ThemeDto;
import com.morago.backend.entity.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ThemeService {
    Page<ThemeDto> listThemes(Pageable pageable, String q);
    ThemeDto createTheme(ThemeDto dto, MultipartFile icon);
    ThemeDto updateTheme(Long id, ThemeDto dto, MultipartFile icon);
    void deleteTheme(Long id);
    Theme findByIdOrThrow(Long id);
    List<ThemeDto> getActiveThemes();
}

