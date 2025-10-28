package com.morago.backend.controller;

import com.morago.backend.dto.call.CallDto;
import com.morago.backend.service.call.CallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/call")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;


    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createCall(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("========================================");
        log.info("CREATE CALL ENDPOINT HIT!");
        log.info("Request body: {}", request);
        log.info("UserDetails: username={}, authorities={}",
                userDetails != null ? userDetails.getUsername() : "NULL",
                userDetails != null ? userDetails.getAuthorities() : "NULL");
        log.info("========================================");

        Long recipientId = getLongValue(request, "recipientId");
        if (recipientId == null) {
            recipientId = getLongValue(request, "translatorId");
        }

        Long themeId = getLongValue(request, "themeId");

        if (recipientId == null) {
            log.error("Neither recipientId nor translatorId found in request: {}", request);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "translatorId or recipientId is required"
            ));
        }

        if (themeId == null) {
            log.error("themeId is null in request: {}", request);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "themeId is required"
            ));
        }

        String callerUsername = userDetails.getUsername();
        log.info("Creating call from {} to translator {} with theme {}", callerUsername, recipientId, themeId);

        try {
            CallDto call = callService.initiateCall(recipientId, themeId, callerUsername);

            log.info("Call created successfully: callId={}", call.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Call initiated successfully",
                    "callId", call.getId(),
                    "id", call.getId(),
                    "recipientId", recipientId,
                    "translatorId", recipientId,
                    "themeId", themeId
            ));
        } catch (Exception e) {
            log.error("Failed to create call", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to create call: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'TRANSLATOR', 'ADMIN')")
    public ResponseEntity<CallDto> getCall(@PathVariable Long id) {
        return ResponseEntity.ok(callService.getCallById(id));
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CallDto>> getAllCalls() {
        return ResponseEntity.ok(callService.getAllCalls());
    }


    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'TRANSLATOR')")
    public ResponseEntity<List<CallDto>> getCallHistory(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return ResponseEntity.ok(callService.getCallHistoryForUser(username));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CallDto> updateCall(
            @PathVariable Long id,
            @RequestBody CallDto dto) {
        return ResponseEntity.ok(callService.updateCall(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCall(@PathVariable Long id) {
        callService.deleteCall(id);
        return ResponseEntity.noContent().build();
    }



    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}