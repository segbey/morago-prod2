package com.morago.backend.controller;

import com.morago.backend.dto.password.ChangePasswordRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileRequestDto;
import com.morago.backend.dto.user.UserUpdateProfileResponseDto;
import com.morago.backend.service.user.UserService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
            description = "Partially update profile fields (allowed for ADMIN or the user themselves).",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserUpdateProfileRequestDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Profile updated",
                            content = @Content(schema = @Schema(implementation = UserUpdateProfileResponseDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<UserUpdateProfileResponseDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateProfileRequestDto dto
    ) {
        return ResponseEntity.ok(userService.updateProfile(id, dto));
    }

    @Operation(
            summary = "Change password",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Password changed"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden")
            },
            description = "User can change own password; admins can change any user's password without current password",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDto dto
    ) {
        userService.changePassword(id, dto);
        return ResponseEntity.noContent().build();
    }
}
