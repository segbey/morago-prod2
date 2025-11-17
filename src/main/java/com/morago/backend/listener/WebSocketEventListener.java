package com.morago.backend.listener;


import com.morago.backend.dto.tokens.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import java.time.LocalDateTime;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        java.security.Principal user = headerAccessor.getUser();
        String username = (user != null) ? user.getName() : "unknown";

        logger.info("WebSocket connected - sessionId: {}, username: {}", sessionId, username);


    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        logger.info("Web socket session disconnected: {} for user: {}", sessionId, userId);

        if (userId != null) {
            NotificationMessage disconnectMessage = NotificationMessage.builder()
                    .type("USER_DISCONNECTED")
                    .sender("System")
                    .text("User " + userId + " has disconnected")
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/user-status", disconnectMessage);
        }
    }
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        java.security.Principal user = headerAccessor.getUser();
        String username = user != null ? user.getName() : "unknown";
        String subscriptionId = headerAccessor.getSubscriptionId();

        logger.info("Client subscribed - sessionId: {}, username: {}, destination: {}, subscriptionId: {}",
                sessionId, username, destination, subscriptionId);
    }

    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        java.security.Principal user = headerAccessor.getUser();
        String username = user != null ? user.getName() : "unknown";
        String subscriptionId = headerAccessor.getSubscriptionId();

        logger.info("Client unsubscribed - sessionId: {}, username: {}, subscriptionId: {}",
                sessionId, username, subscriptionId);
    }
}