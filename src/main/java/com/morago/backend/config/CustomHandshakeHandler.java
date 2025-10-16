package com.morago.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomHandshakeHandler.class);

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        Long userId = (Long) attributes.get("userId");
        String username = (String) attributes.get("username");
        String role = (String) attributes.get("role");

        if (userId != null && username != null && role != null) {
            log.info("Creating UserPrincipal for user: {} with role: {}", username, role);
            return new UserPrincipal(userId, username, role);
        }

        if (username != null && !username.isBlank()) {
            log.info("Creating StompPrincipal fallback for user: {}", username);
            return new StompPrincipal(username);
        }

        log.warn("No valid username found in handshake attributes");
        return null;
    }

    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }

    public static class UserPrincipal implements Principal {
        private final Long userId;
        private final String username;
        private final String role;

        public UserPrincipal(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        @Override
        public String getName() {
            return username;
        }

        public Long getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }
    }
}
