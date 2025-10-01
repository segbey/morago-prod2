package com.morago.backend.controller;

import com.morago.backend.dto.tokens.WebRTCSignalMessage;
import com.morago.backend.service.webrtc.WebRTCService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class WebRTCController {

    private final WebRTCService webRTCService;

    @MessageMapping("/webrtc.init/{callId}")
    public void initializePeerConnection(@DestinationVariable String callId,
                                         SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getUser() != null ?
                headerAccessor.getUser().getName() : "anonymous";

        webRTCService.initializePeerConnection(callId, userId);
    }


    @MessageMapping("/webrtc.close/{callId}")
    public void closePeerConnection(@DestinationVariable String callId,
                                    SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getUser() != null ?
                headerAccessor.getUser().getName() : "anonymous";

        webRTCService.closePeerConnection(callId, userId);
    }
}