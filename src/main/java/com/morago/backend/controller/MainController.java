package com.morago.backend.controller;

import com.morago.backend.config.utils.CookieUtils;
import com.morago.backend.dto.auth.AuthResponse;
import com.morago.backend.dto.tokens.AccessTokenResponse;
import com.morago.backend.dto.tokens.JWTRequest;
import com.morago.backend.dto.user.UserRegistrationRequestDto;
import com.morago.backend.dto.user.UserRegistrationResponseDto;
import com.morago.backend.dto.tokens.AuthTokens;
import com.morago.backend.dto.tokens.RotatedTokens;
import com.morago.backend.service.auth.AuthService;
import com.morago.backend.service.token.RefreshTokenService;
import com.morago.backend.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Authentication", description = "User authentication and token management. Access: [USER, TRANSLATOR, ADMIN]")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class MainController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @Operation(
            summary = "Log in with phone and password",
            description = "Authenticates the user and returns an access token and user data in the body. Refresh token is set as an HttpOnly cookie.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = JWTRequest.class),
                            examples = @ExampleObject(
                                    name = "Example",
                                    value = "{\"username\":\"01012345678\",\"password\":\"P@ssw0rd!\"}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful authentication",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "HttpOnly refresh token cookie"
                            ),
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid username or password")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody JWTRequest authRequest,
                                              HttpServletResponse servletResponse) {
        AuthTokens tokens = authService.createAuthToken(authRequest);

        boolean secure = false;
        String sameSite = "Lax";
        String path = "/auth";

        ResponseCookie cookie = CookieUtils.refreshCookie(
                tokens.getRefreshToken(),
                tokens.getRefreshExpiresAt(),
                path,
                secure,
                sameSite
        );

        log.info("Set-Cookie (login): {}", cookie.toString());

        AuthResponse body = AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .user(tokens.getUser())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Reads the refresh token from an HttpOnly cookie, rotates it, and returns a new access token in the body.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "New access token generated",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "Rotated HttpOnly refresh token cookie"
                            ),
                            content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token cookie")
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refreshToken(
            @CookieValue(name = CookieUtils.REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RotatedTokens rotated = refreshTokenService.refreshTokens(refreshToken);

        ResponseCookie cookie = CookieUtils.refreshCookie(
                rotated.newRefreshToken(),
                rotated.refreshExpiresAt(),
                "/auth",
                true,
                "Lax"
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AccessTokenResponse(rotated.newAccessToken()));
    }

    @Operation(
            summary = "Register user",
            description = "Registers a new user with role ROLE_USER. Phone format strictly 01012345678.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User created",
                            content = @Content(schema = @Schema(implementation = UserRegistrationResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Phone already registered")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDto> registerUser(@Valid @RequestBody UserRegistrationRequestDto dto) {
        return ResponseEntity.status(201).body(userService.registerUser(dto));
    }

    @Operation(
            summary = "Register translator",
            description = "Registers a new translator with role ROLE_TRANSLATOR. Phone format 01012345678.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Translator created",
                            content = @Content(schema = @Schema(implementation = UserRegistrationResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Phone already registered")
            }
    )
    @PostMapping("/register/translator")
    public ResponseEntity<UserRegistrationResponseDto> registerTranslator(@Valid @RequestBody UserRegistrationRequestDto dto) {
        return ResponseEntity.status(201).body(userService.registerTranslator(dto));
    }

    @Operation(
            summary = "Log out",
            description = "Invalidates refresh tokens and deletes the refresh cookie. Requires a valid Bearer token.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Logged out (no content)"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            Authentication auth,
            @CookieValue(name = CookieUtils.REFRESH_COOKIE, required = false) String refreshTokenCookie
    ) {
        String username = auth.getName();
        log.info("logout: user={}, cookiePresent={}, cookiePrefix={}",
                username,
                refreshTokenCookie != null && !refreshTokenCookie.isBlank(),
                refreshTokenCookie == null ? "null" : refreshTokenCookie.substring(0, Math.min(12, refreshTokenCookie.length()))
        );

        refreshTokenService.logout(username, refreshTokenCookie);

        ResponseCookie delete = CookieUtils.deleteRefreshCookie(
                "/auth",
                true,
                "Lax"
        );
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, delete.toString())
                .build();
    }
}
