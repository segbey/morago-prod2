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
    private String type;
    private Long callId;
    private String fromUserId;
    private String toUserId;
    private Object sdp;
    private Object candidate; }