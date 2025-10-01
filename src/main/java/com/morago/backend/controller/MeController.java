package com.morago.backend.controller;

import com.morago.backend.dto.FileResponse;
import com.morago.backend.dto.password.ChangePasswordRequestDto;
import com.morago.backend.service.file.FileService;
import com.morago.backend.service.user.UserService;
import com.morago.backend.dto.NotificationDto;
import com.morago.backend.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Manage current user's profile. Access: [USER, TRANSLATOR, ADMIN]")
public class MeController {

    private final UserService userService;
    private final FileService fileService;
    private final NotificationService notificationService;

    @Operation(
            summary = "Change password",
            description = "Allows an authenticated user to change their password.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChangePasswordRequestDto.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Password successfully changed"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden")
            }
    )
    @PatchMapping("/password")
    @PreAuthorize("hasAnyRole('USER','TRANSLATOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeMyPassword(
            @Valid @RequestBody ChangePasswordRequestDto dto) {
        userService.changeMyPassword(dto);
    }

    @Operation(
            summary = "Upload or update avatar",
            description = "Allows an authenticated user to upload or replace their profile avatar.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Avatar saved",
                            content = @Content(schema = @Schema(implementation = FileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "File upload error"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden")
            }
    )
    @RequestMapping(
            value = "/avatar",
            method = { RequestMethod.POST, RequestMethod.PUT },
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FileResponse uploadMyAvatar(
            @Parameter(description = "Image file (jpg, png, etc.)", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        return fileService.uploadMyAvatar(file);
    }

    @Operation(
            summary = "Delete avatar",
            description = "Allows an authenticated user to delete their current avatar.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Avatar deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Avatar not found")
            }
    )
    @DeleteMapping("/avatar")
    @PreAuthorize("hasAnyRole('USER','TRANSLATOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAvatar() {
        fileService.deleteMyAvatar();
    }

    @Operation(
            summary = "Get my notifications",
            description = "Retrieve all notifications for the currently authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notifications retrieved",
                            content = @Content(schema = @Schema(implementation = NotificationDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('USER','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<NotificationDto>> getMyNotifications() {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId));
    }



    @Operation(
            summary = "Clear my notifications",
            description = "Deletes all notifications for the currently authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Notifications cleared"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @DeleteMapping("/notifications")
    @PreAuthorize("hasAnyRole('USER','TRANSLATOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMyNotifications() {
        Long userId = userService.getCurrentUserId();
        notificationService.clearNotificationsForUser(userId);
    }
}
