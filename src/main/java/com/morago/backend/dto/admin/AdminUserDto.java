package com.morago.backend.dto.admin;


import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for creating a new user by an admin")

public class AdminUserDto {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    @Schema(example = "John")
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
    @Size(min = 8, max = 72, message = "Password must be 8–72 chars")
    @Schema(example = "P@ssw0rd!", minLength = 8, maxLength = 72)
    private String password;

    @Schema(example = "10000.00", description = "Initial balance for the user (defaults to 0 if omitted)")
    @PositiveOrZero(message = "Balance cannot be negative")
    private BigDecimal balance;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
