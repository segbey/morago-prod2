package com.morago.backend.service.auth;

import com.morago.backend.config.utils.JWTUtils;
import com.morago.backend.dto.auth.AuthResponse;
import com.morago.backend.dto.tokens.JWTRequest;
import com.morago.backend.dto.user.UserDto;
import com.morago.backend.entity.User;
import com.morago.backend.mapper.UserMapper;
import com.morago.backend.service.profile.TranslatorProfileService;
import com.morago.backend.service.token.RefreshTokenService;
import com.morago.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JWTUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final TranslatorProfileService translatorProfileService;
    private final UserMapper userMapper;

    @Override
    public AuthResponse createAuthToken(JWTRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();

        String accessToken  = jwtUtils.generateAccessToken(principal);
        String refreshToken = jwtUtils.generateRefreshToken(principal);

        refreshTokenService.createRefreshToken(principal.getUsername(), refreshToken);

        User user = userService.findByUsernameOrThrow(authRequest.getUsername());
        translatorProfileService.setOnlineStatus(user, true);

        UserDto userDto = userMapper.toDto(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userDto)
                .build();
    }
}
