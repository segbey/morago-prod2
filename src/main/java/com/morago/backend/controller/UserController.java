package com.morago.backend.controller;

import com.morago.backend.dto.UserProfileDto;
import com.morago.backend.dto.user.UserResponseDto;
import com.morago.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {
    private final UserService userService;

    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    @Operation(
            summary = "Update user profile",
            description = "Partially update profile fields (allowed for ADMIN or the user themself)."
    )
    @ApiResponse(responseCode = "200", description = "Profile updated")
    public ResponseEntity<UserResponseDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserProfileDto dto
    ) {
        return ResponseEntity.ok(userService.updateProfile(id, dto));
    }
}
