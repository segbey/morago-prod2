package com.morago.backend.dto.translator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorBasicInfoRequestDto {

    @Email(message = "Invalid email format")
    @Schema(description = "Email address", example = "translator@example.com")
    private String email;

    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Date of birth", example = "1990-01-15")
    private LocalDate dateOfBirth;
}