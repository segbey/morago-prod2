package com.morago.backend.dto.translator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorBasicInfoResponseDto {
    private String email;
    private LocalDate dateOfBirth;
    private String message;
}