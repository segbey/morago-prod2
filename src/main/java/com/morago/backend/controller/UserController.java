package com.morago.backend.controller;

import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.service.profile.TranslatorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {
    private final TranslatorProfileService translatorProfileService;

    @Operation(
            summary = "Get online translators",
            description = "Get list of currently online and verified translators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Online translators retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @GetMapping("/online")
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
    @GetMapping("/by-theme/{themeId}")
    @PreAuthorize("hasAnyRole('USER', 'TRANSLATOR')")
    public ResponseEntity<List<TranslatorProfileDto>> getTranslatorsByTheme(@PathVariable Long themeId) {
        return ResponseEntity.ok(translatorProfileService.getTranslatorsByTheme(themeId));
    }
}
