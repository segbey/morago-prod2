package com.morago.backend.dto.translator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorLanguagesResponseDto {
    private Set<Long> languageIds;
    private String message;
}