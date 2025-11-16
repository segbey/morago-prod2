package com.morago.backend.controller;

import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {
    private final TranslatorProfileService translatorProfileService;
    private final UserService userService;

    @Operation(
            summary = "Get online translators",
            description = "Get list of currently online and verified translators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Online translators retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @GetMapping("/translators/online")
    @PreAuthorize("hasAnyRole('USER', 'TRANSLATOR')")
    public ResponseEntity<List<TranslatorProfileDto>> getOnlineTranslators() {
        return ResponseEntity.ok(translatorProfileService.getOnlineTranslators());
    }

    @Operation(
            summary = "Get translators by theme",
            description = "Get translators who handle a specific theme.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translators retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @GetMapping("/translators/online/by-theme/{themeId}")
    @PreAuthorize("hasAnyRole('USER', 'TRANSLATOR')")
    public ResponseEntity<List<TranslatorProfileDto>> getTranslatorsByTheme(@PathVariable Long themeId) {
        return ResponseEntity.ok(translatorProfileService.getTranslatorsByTheme(themeId));
    }

    @Operation(
            summary = "Get translators by language",
            description = "Get translators who speak a specific language.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translators retrieved successfully")
            }
    )
    @GetMapping("/translators/online/by-language/{languageId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<TranslatorProfileDto> getTranslatorsByLanguage(@PathVariable Long languageId) {
        return translatorProfileService.getTranslatorsByLanguage(languageId);
    }

    @Operation(summary = "Get translator by ID", description = "Get a specific translator's profile.")
    @GetMapping("/translator/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TranslatorProfileDto> getTranslatorById(@PathVariable Long id) {
        TranslatorProfileDto dto = translatorProfileService.getById(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Update my profile",
            description = "Update profile of currently authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @PutMapping("/update/profile")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<UserUpdateProfileResponseDto> updateMyProfile(
            @Valid @RequestBody UserUpdateProfileRequestDto dto
    ) {
        UserUpdateProfileResponseDto response = userService.updateMyProfile(dto);
        return ResponseEntity.ok(response);
    }
}
