package com.morago.backend.dto.translator;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslatorAvatarUpdateRequest {
    @NotNull(message = "Avatar file is required")
    private MultipartFile avatarFile;
}