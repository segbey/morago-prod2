package com.morago.backend.dto.tokens;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRTCSignalMessage {
    private String type; // OFFER, ANSWER, ICE_CANDIDATE
    private Long callId;
    private String fromUserId;
    private String toUserId;
    private Object sdp; // RTCSessionDescriptionInit for OFFER/ANSWER
    private Object candidate; // RTCIceCandidateInit for ICE_CANDIDATE
}