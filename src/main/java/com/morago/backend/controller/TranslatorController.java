package com.morago.backend.controller;

import com.morago.backend.dto.TranslatorProfileDto;
import com.morago.backend.service.profile.TranslatorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/translators")
@RequiredArgsConstructor
public class TranslatorController {

    private final TranslatorProfileService translatorProfileService;

    @Operation(
            summary = "Get all translators",
            description = "Get paginated list of all translators. Admin only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translators retrieved successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
            }
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<TranslatorProfileDto> getAllTranslators(Pageable pageable) {
        return translatorProfileService.getAll(pageable);
    }

    @Operation(
            summary = "Get online translators",
            description = "Get list of currently online translators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Online translators retrieved successfully")
            }
    )
    @GetMapping("/online")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<TranslatorProfileDto> getOnlineTranslators() {
        return translatorProfileService.getOnlineTranslators();
    }

    @Operation(
            summary = "Get translators by theme",
            description = "Get translators who handle a specific theme.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translators retrieved successfully")
            }
    )
    @GetMapping("/by-theme/{themeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<TranslatorProfileDto> getTranslatorsByTheme(@PathVariable Long themeId) {
        return translatorProfileService.getTranslatorsByTheme(themeId);
    }

    @Operation(
            summary = "Get translators by language",
            description = "Get translators who speak a specific language.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translators retrieved successfully")
            }
    )
    @GetMapping("/by-language/{languageId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<TranslatorProfileDto> getTranslatorsByLanguage(@PathVariable Long languageId) {
        return translatorProfileService.getTranslatorsByLanguage(languageId);
    }

    @Operation(
            summary = "Get translator by ID",
            description = "Get a specific translator's profile.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translator retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Translator not found")
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    public ResponseEntity<TranslatorProfileDto> getTranslatorById(@PathVariable Long id) {
        return ResponseEntity.ok(translatorProfileService.getById(id));
    }

    @Operation(
            summary = "Update translator profile",
            description = "Update a translator's profile. Admin or translator themselves only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Translator not found")
            }
    )
    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TranslatorProfileDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody TranslatorProfileDto dto
    ) {
        return ResponseEntity.ok(translatorProfileService.update(id, dto));
    }

    @Operation(
            summary = "Verify translator",
            description = "Verify a translator's credentials. Admin only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translator verified successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Translator not found")
            }
    )
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TranslatorProfileDto> verifyTranslator(@PathVariable Long id) {
        TranslatorProfileDto translator = translatorProfileService.getById(id);
        translator.setIsVerified(true);
        return ResponseEntity.ok(translatorProfileService.update(id, translator));
    }

    @Operation(
            summary = "Delete translator",
            description = "Delete a translator profile. Admin only.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Translator deleted successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Translator not found")
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTranslator(@PathVariable Long id) {
        translatorProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
