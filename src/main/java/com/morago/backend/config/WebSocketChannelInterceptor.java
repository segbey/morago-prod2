package com.morago.backend.config;

import com.morago.backend.service.call.CallAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final CallAccessService callAccessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();

            if (destination != null && (destination.startsWith("/app/webrtc.") ||
                    destination.equals("/app/call.accept") ||
                    destination.equals("/app/call.reject") ||
                    destination.equals("/app/call.end"))) {

                Object payload = message.getPayload();
                if (payload instanceof byte[]) {
                    log.debug("WebRTC message to {}: {}", destination, new String((byte[]) payload));
                }
            }
        }

        return message;
    }
}
