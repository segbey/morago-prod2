package com.morago.backend.config;

import com.morago.backend.config.utils.JWTUtils;
import com.morago.backend.service.call.CallAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebSocketBeanConfig {

    private final JWTUtils jwtUtils;
    private final CallAccessService callAccessService;

    @Bean
    public JwtHandshakeInterceptor jwtHandshakeInterceptor() {
        return new JwtHandshakeInterceptor(jwtUtils);
    }

    @Bean
    public CustomHandshakeHandler customHandshakeHandler() {
        return new CustomHandshakeHandler();
    }

    @Bean
    public WebSocketChannelInterceptor webSocketChannelInterceptor() {
        return new WebSocketChannelInterceptor(callAccessService);
    }
}