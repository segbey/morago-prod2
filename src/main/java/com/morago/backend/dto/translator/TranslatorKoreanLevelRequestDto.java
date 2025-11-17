package com.morago.backend.dto.translator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorKoreanLevelRequestDto {

    @Size(max = 200, message = "Korean level must not exceed 200 characters")
    @Schema(description = "Level of Korean proficiency", example = "Advanced")
    private String levelOfKorean;
}