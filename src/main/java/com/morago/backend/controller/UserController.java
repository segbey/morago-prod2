package com.morago.backend.controller;

import com.morago.backend.dto.FileResponse;
import com.morago.backend.dto.password.ChangePasswordRequestDto;
import com.morago.backend.dto.translator.TranslatorProfileDto;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.service.file.FileService;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.user.UserService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {
    private final UserService userService;
    private final TranslatorProfileService translatorProfileService;

    @Operation(
            summary = "Delete profile",
            description = "Delete user's profile (soft delete).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Profile deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - USER role required")
            }
    )
    @DeleteMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile() {
        Long userId = userService.getCurrentUserId();
        userService.deleteUser(userId);
    }

    @GetMapping("/translators/themes/{themeId}")
    public ResponseEntity<List<TranslatorProfileDto>> getTranslatorsByTheme(@PathVariable Long themeId) {
        return ResponseEntity.ok(translatorProfileService.getTranslatorsByTheme(themeId));
    }


}
