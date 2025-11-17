package com.morago.backend.dto.translator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorAvailabilityResponseDto {
    private Boolean isOnline;
    private String message;
}