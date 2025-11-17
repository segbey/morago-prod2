package com.morago.backend.dto.translator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorAvailabilityRequestDto {

    @NotNull(message = "Online status is required")
    @Schema(description = "Translator online status", example = "true")
    private Boolean isOnline;
}