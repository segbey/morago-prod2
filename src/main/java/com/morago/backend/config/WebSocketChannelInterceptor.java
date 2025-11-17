package com.morago.backend.config;

import com.morago.backend.service.call.CallAccessService;
import lombok.NonNull;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final CallAccessService callAccessService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            Principal principal = accessor.getUser();
            String username = principal != null ? principal.getName() : "anonymous";
            String sessionId = accessor.getSessionId();

            if (StompCommand.SUBSCRIBE.equals(command)) {
                String destination = accessor.getDestination();
                String subscriptionId = accessor.getSubscriptionId();
                log.info("SUBSCRIBE - sessionId: {}, username: {}, destination: {}, subscriptionId: {}",
                        sessionId, username, destination, subscriptionId);
            } else if (StompCommand.UNSUBSCRIBE.equals(command)) {
                String subscriptionId = accessor.getSubscriptionId();
                log.info("UNSUBSCRIBE - sessionId: {}, username: {}, subscriptionId: {}",
                        sessionId, username, subscriptionId);
            } else if (StompCommand.MESSAGE.equals(command)) {
                String destination = accessor.getDestination();
                log.debug("MESSAGE - sessionId: {}, destination: {}", sessionId, destination);
            } else if (StompCommand.SEND.equals(command)) {
                String destination = accessor.getDestination();

                if (destination != null && (destination.startsWith("/app/webrtc.") ||
                        destination.equals("/app/call.accept") ||
                        destination.equals("/app/call.reject") ||
                        destination.equals("/app/call.end"))) {

                    Object payload = message.getPayload();
                    if (payload instanceof byte[]) {
                        log.debug("SEND - sessionId: {}, username: {}, destination: {}, payload: {}",
                                sessionId, username, destination, new String((byte[]) payload));
                    }
                }
            }
        }

            return message;
        }

}
