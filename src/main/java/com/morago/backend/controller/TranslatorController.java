package com.morago.backend.controller;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.mapper.WithdrawalMapper;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.user.UserService;
import com.morago.backend.service.withdrawal.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    private final UserService userService;

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

    @Operation(summary = "Get translator by ID", description = "Get a specific translator's profile.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TranslatorProfileDto> getTranslatorById(@PathVariable Long id) {
        TranslatorProfileDto dto = translatorProfileService.getById(id);
        return ResponseEntity.ok(dto);
    }
}