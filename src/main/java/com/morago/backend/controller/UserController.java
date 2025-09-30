package com.morago.backend.controller;

import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.service.profile.TranslatorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(
        name = "Users",
        description = "User management. Access: [USER] (search for translators)."
)
public class UserController {
    private final TranslatorProfileService service;

    @Operation(
            summary = "Search translators",
            description = "Access: [USER]\nSearch for translators by language, theme, online/verified status, with sorting and pagination."
    )
    @GetMapping("/translators")
    public Page<TranslatorProfileDto> searchTranslators(
            @RequestParam List<Long> languageIds,
            @RequestParam(required = false) Long themeId,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) Boolean verified,
            @ParameterObject
            @PageableDefault(page = 0, size = 20)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "ratingAvg", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "ratingCount", direction = Sort.Direction.DESC)
            })
            Pageable pageable
    ) {
        return service.searchTranslators(languageIds, themeId, online, verified, pageable);
    }
}
