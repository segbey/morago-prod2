package com.morago.backend.service.theme;

import com.morago.backend.dto.ThemeDto;
import com.morago.backend.entity.Theme;

import java.util.List;

public interface ThemeService {
    ThemeDto create(ThemeDto dto);
    ThemeDto update(Long id, ThemeDto dto);
    void delete(Long id);
    ThemeDto getById(Long id);
    List<ThemeDto> getAll();
    Theme findByIdOrThrow(Long id);
}
