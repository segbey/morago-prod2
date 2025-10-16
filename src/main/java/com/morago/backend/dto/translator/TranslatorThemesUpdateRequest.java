package com.morago.backend.dto.translator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorThemesUpdateRequest {
    @NotNull(message = "Theme IDs are required")
    private Set<Long> themeIds;
}