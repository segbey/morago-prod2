package com.morago.backend.config;

import com.morago.backend.config.utils.JWTUtils;
import com.morago.backend.entity.enumFiles.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;
import java.util.Set;


@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JWTUtils jwtUtils;

    public JwtHandshakeInterceptor(JWTUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            String token = extractTokenFromQuery(request);

            if (token == null) {
                log.warn("No token found in WebSocket handshake");
                return false;
            }

            jwtUtils.validateToken(token, TokenType.ACCESS);

            String username = jwtUtils.getUsernameFromToken(token, TokenType.ACCESS);
            Long userId = jwtUtils.getUserIdFromToken(token, TokenType.ACCESS);
            Set<String> roles = jwtUtils.getRolesFromToken(token, TokenType.ACCESS);
            if (username == null || username.isBlank()) {
                log.warn("Invalid username in JWT token");
                return false;
            }

            attributes.put("username", username);
            attributes.put("token", token);
            attributes.put("userId", userId);
            attributes.put("roles", roles);

            log.info("WebSocket handshake successful for user: {}", username);
            return true;

        } catch (Exception e) {
            log.error("WebSocket handshake failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake error: {}", exception.getMessage());
        }
    }

    private String extractTokenFromQuery(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String queryString = servletRequest.getServletRequest().getQueryString();
            if (queryString != null && queryString.contains("token=")) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        String token = param.substring(6);
                        return java.net.URLDecoder.decode(token, java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
        }
        return null;
    }
}