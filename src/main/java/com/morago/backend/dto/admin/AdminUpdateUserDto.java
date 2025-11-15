package com.morago.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for updating a user by an admin (all fields optional; send only what you want to change)")
public class AdminUpdateUserDto {

    @Size(min = 2, max = 50, message = "First name must be 2–50 characters")
    @Schema(example = "John")
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


    @PositiveOrZero(message = "Balance cannot be negative")
    @Schema(example = "15000.00", description = "If present, sets the user's balance to this value")
    private BigDecimal balance;
}

