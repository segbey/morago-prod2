package com.morago.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for creating a translator (admin)")
public class AdminTranslatorDto {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    @Schema(example = "Jane")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2–50 characters")
    @Schema(example = "Doe")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @JsonAlias({"phone_number", "phone", "phoneNo"})
    @Schema(
            example = "01012345678",
            pattern = "^010\\d{8}$",
            description = "Korean mobile: exactly 11 digits, no dashes/spaces, must start with 010"
    )
    @Pattern(
            regexp = "^010\\d{8}$",
            message = "Phone must be 11 digits and start with 010 (e.g. 01012345678)"
    )
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
    @Schema(example = "P@ssw0rd!", minLength = 8, maxLength = 72)
    private String password;

    @Past(message = "Date of birth must be in the past")
    @Schema(example = "1995-03-21", type = "string", format = "date")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Level of Korean is required")
    @Schema(example = "3 Level", description = "Translator's Korean proficiency level (TOPIK)")
    private String levelOfKorean;

    @NotNull(message = "At least one language is required")
    @Size(min = 1, message = "Select at least one language")
    @Schema(example = "[1,2]", description = "IDs of languages the translator supports")
    private List<Long> languageIds;

    @NotNull(message = "At least one theme is required")
    @Size(min = 1, message = "Select at least one theme")
    @Schema(example = "[10,12]", description = "IDs of themes the translator supports")
    private List<Long> themeIds;
}
