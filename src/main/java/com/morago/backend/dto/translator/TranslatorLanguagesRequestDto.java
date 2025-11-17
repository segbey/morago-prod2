package com.morago.backend.dto.translator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorLanguagesRequestDto {

    @NotEmpty(message = "At least one language ID is required")
    @Schema(description = "Set of language IDs", example = "[1, 2, 3]")
    private Set<Long> languageIds;
}