package com.morago.backend.dto.translator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorStatusUpdateRequest {
    @NotNull(message = "Online status is required")
    private Boolean isOnline;
}