package com.morago.backend.controller;

import com.morago.backend.dto.call.CallActionRequest;
import com.morago.backend.dto.tokens.CallNotificationMessage;
import com.morago.backend.dto.tokens.WebRTCSignalMessage;
import com.morago.backend.service.call.CallAccessService;
import com.morago.backend.service.call.CallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CallSignalingController {

    private final CallAccessService callAccessService;
    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/webrtc.offer")
    @Transactional
    public void handleOffer(@Payload WebRTCSignalMessage message, Principal principal) {
        log.info("Received OFFER for call {} from {}", message.getCallId(), principal.getName());
        callAccessService.validateTargetUser(
                message.getCallId(),
                message.getFromUserId(),
                message.getToUserId(),
                principal
        );
        messagingTemplate.convertAndSendToUser(
                message.getToUserId(),
                "/queue/webrtc-signals",
                message
        );
        log.info("Forwarded OFFER to user {}", message.getToUserId());
    }

    @MessageMapping("/webrtc.answer")
    @Transactional
    public void handleAnswer(@Payload WebRTCSignalMessage message, Principal principal) {
        log.info("Received ANSWER for call {} from {}", message.getCallId(), principal.getName());
        callAccessService.validateTargetUser(
                message.getCallId(),
                message.getFromUserId(),
                message.getToUserId(),
                principal
        );
        messagingTemplate.convertAndSendToUser(
                message.getToUserId(),
                "/queue/webrtc-signals",
                message
        );
        log.info("Forwarded ANSWER to user {}", message.getToUserId());
    }

    @MessageMapping("/webrtc.ice")
    @Transactional
    public void handleIceCandidate(@Payload WebRTCSignalMessage message, Principal principal) {
        log.debug("Received ICE candidate for call {} from {}", message.getCallId(), principal.getName());
        callAccessService.validateTargetUser(
                message.getCallId(),
                message.getFromUserId(),
                message.getToUserId(),
                principal
        );
        messagingTemplate.convertAndSendToUser(
                message.getToUserId(),
                "/queue/webrtc-signals",
                message
        );
    }

    @MessageMapping("/call.accept")
    @Transactional
    public void acceptCall(@Payload CallActionRequest request, Principal principal) {
        log.info("Call {} accepted by {}", request.getCallId(), principal.getName());
        callAccessService.validateCallAccess(request.getCallId(), principal);

        String callIdStr = request.getCallId().toString();
        callService.acceptCall(callIdStr, principal.getName());

        String otherUser = callAccessService.getOtherParticipant(request.getCallId(), principal);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .type("CALL_ACCEPTED")
                .callId(callIdStr)
                .build();

        messagingTemplate.convertAndSendToUser(otherUser, "/queue/call-notifications", notification);
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/call-notifications", notification);
    }

    @MessageMapping("/call.reject")
    @Transactional
    public void rejectCall(@Payload CallActionRequest request, Principal principal) {
        log.info("Call {} rejected by {}", request.getCallId(), principal.getName());

        callAccessService.validateCallAccess(request.getCallId(), principal);

        String callIdStr = request.getCallId().toString();
        callService.rejectCall(callIdStr, principal.getName());
        String otherUser = callAccessService.getOtherParticipant(request.getCallId(), principal);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .type("CALL_REJECTED")
                .callId(callIdStr)
                .build();

        messagingTemplate.convertAndSendToUser(otherUser, "/queue/call-notifications", notification);
    }

    @MessageMapping("/call.end")
    @Transactional
    public void endCall(@Payload CallActionRequest request, Principal principal) {
        log.info("Call {} ended by {}", request.getCallId(), principal.getName());
        callAccessService.validateCallAccess(request.getCallId(), principal);

        String callIdStr = request.getCallId().toString();
        callService.endCall(callIdStr, principal.getName());
        String otherUser = callAccessService.getOtherParticipant(request.getCallId(), principal);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .type("CALL_ENDED")
                .callId(callIdStr)
                .build();

        messagingTemplate.convertAndSendToUser(otherUser, "/queue/call-notifications", notification);
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/call-notifications", notification);
    }
}