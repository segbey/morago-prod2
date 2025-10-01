package com.morago.backend.service.call;

import com.morago.backend.entity.Call;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.Roles;
import com.morago.backend.exception.ApiException;
import com.morago.backend.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallAccessService {

    private final CallRepository callRepository;

    public void validateCallAccess(Long callId, Principal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "No principal found");
        }

        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CALL_NOT_FOUND", "Call not found: " + callId));

        String username = principal.getName(); // phone number from JWT

        boolean isParticipant = Objects.equals(call.getCaller().getUsername(), username) ||
                Objects.equals(call.getRecipient().getUsername(), username);

        if (!isParticipant) {
            log.warn("User {} attempted to access call {} without permission", username, callId);
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not authorized to access this call");
        }
    }

    public void validateTargetUser(Long callId, String fromUserId, String toUserId, Principal principal) {
        validateCallAccess(callId, principal);

        Call call = callRepository.findById(callId).orElseThrow();


        String callerPhone = call.getCaller().getUsername();
        String recipientPhone = call.getRecipient().getUsername();

        if (!Objects.equals(fromUserId, principal.getName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "fromUserId must match authenticated user");
        }

        boolean validTarget = (Objects.equals(fromUserId, callerPhone) && Objects.equals(toUserId, recipientPhone)) ||
                (Objects.equals(fromUserId, recipientPhone) && Objects.equals(toUserId, callerPhone));

        if (!validTarget) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Invalid target user for this call");
        }
    }

    public void validateCanInitiateCall(User user) {
        boolean hasUserRole = user.getRoles().stream()
                .anyMatch(role -> role.getName() == Roles.ROLE_USER);

        if (!hasUserRole) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only users with ROLE_USER can initiate calls");
        }
    }

    public String getOtherParticipant(Long callId, Principal principal) {
        Call call = callRepository.findById(callId).orElseThrow();
        String myPhone = principal.getName();


        if (Objects.equals(call.getCaller().getUsername(), myPhone)) {
            return call.getRecipient().getUsername();
        } else {
            return call.getCaller().getUsername();
        }
    }
}
