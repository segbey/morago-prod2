package com.morago.backend.controller;

import com.morago.backend.dto.TranslatorProfileDto;
import com.morago.backend.service.profile.TranslatorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translators")
@RequiredArgsConstructor
public class TranslatorController {

    private final TranslatorProfileService translatorProfileService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isTranslatorSelf(#id)")
    public ResponseEntity<TranslatorProfileDto> getTranslatorById(@PathVariable Long id) {
        return ResponseEntity.ok(translatorProfileService.getById(id));
    }

    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN') or @authz.isTranslatorSelf(#id)")
    public ResponseEntity<TranslatorProfileDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody TranslatorProfileDto dto
    ) {
        return ResponseEntity.ok(translatorProfileService.update(id, dto));
    }
}
