package com.morago.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for updating a translator by an admin (all fields optional)")
public class AdminUpdateTranslatorDto {

    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    @Schema(example = "Jane")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be 2–50 characters")
    @Schema(example = "Doe")
    private String lastName;

    @JsonAlias({"phone_number", "phone", "phoneNo"})
    @Pattern(
            regexp = "^010\\d{8}$",
            message = "Phone must be 11 digits and start with 010 (e.g. 01012345678)"
    )
    @Schema(example = "01012345678",
            description = "New phone. If present, must be 11 digits and start with 010")
    private String phoneNumber;

    @Size(min = 8, max = 72, message = "Password must be 8–72 chars")
    @Schema(example = "NewP@ssw0rd!", description = "If present and non-blank, will be encoded and set")
    private String password;

    @Schema(example = "1995-03-21", type = "string", format = "date")
    private LocalDate dateOfBirth;

    @Schema(example = "3 LEVEL", description = "Translator's Korean proficiency level (TOPIK or your enum/string). Will be upper-cased if provided.")
    private String levelOfKorean;

    @Schema(example = "[1,2]", description = "Replace languages with these IDs (if provided)")
    private List<Long> languageIds;

    @Schema(example = "[10,12]", description = "Replace themes with these IDs (if provided)")
    private List<Long> themeIds;
}

