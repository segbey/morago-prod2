package com.morago.backend.service.call;

import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.tokens.CallNotificationMessage;
import com.morago.backend.entity.Call;
import com.morago.backend.entity.Theme;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.CallStatus;
import com.morago.backend.mapper.CallMapper;
import com.morago.backend.repository.CallRepository;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.service.debtor.DebtorService;
import com.morago.backend.service.deposit.DepositService;
import com.morago.backend.service.theme.ThemeService;
import com.morago.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CallServiceImpl implements CallService {

    private final CallRepository callRepository;
    private final UserService userService;
    private final ThemeService themeService;
    private final CallMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final DepositService depositService;
    private final DebtorService debtorService;

    private final Map<String, Call> activeCalls = new ConcurrentHashMap<>();

    private <T> T findOrThrow(java.util.Optional<T> optional, String entityName, Long id) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(entityName + " not found with id " + id));
    }

    @Override
    public CallDto getCallById(Long id) {
        return mapper.toDto(findOrThrow(callRepository.findById(id), "Call", id));
    }

    @Override
    public List<CallDto> getAllCalls() {
        return callRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public CallDto updateCall(Long id, CallDto dto) {
        Call call = findOrThrow(callRepository.findById(id), "Call", id);

        call.setDuration(dto.getDuration());
        call.setStatus(dto.isStatus());
        call.setSumDecimal(dto.getSumDecimal());
        call.setCommission(dto.getCommission());
        call.setTranslatorHasJoined(dto.isTranslatorHasJoined());
        call.setUserHasRated(dto.isUserHasRated());
        call.setCallStatus(dto.getCallStatus());
        call.setEndCall(dto.isEndCall());
        call.setChannelName(dto.getChannelName());

        if (dto.getCallerId() != null
                && (call.getCaller() == null || !dto.getCallerId().equals(call.getCaller().getId()))) {
            call.setCaller(userService.findByIdOrThrow(dto.getCallerId()));
        }
        if (dto.getRecipientId() != null
                && (call.getRecipient() == null || !dto.getRecipientId().equals(call.getRecipient().getId()))) {
            call.setRecipient(userService.findByIdOrThrow(dto.getRecipientId()));
        }
        if (dto.getThemeId() != null
                && (call.getTheme() == null || !dto.getThemeId().equals(call.getTheme().getId()))) {
            var theme = themeService.findByIdOrThrow(dto.getThemeId());
            if (!theme.isActive()) {
                throw new IllegalStateException("Theme is inactive");
            }
            call.setTheme(theme);
        }

        return mapper.toDto(callRepository.save(call));
    }

    @Override
    public void deleteCall(Long id) {
        if (!callRepository.existsById(id)) {
            throw new ResourceNotFoundException("Call not found with id " + id);
        }
        callRepository.deleteById(id);
    }

    @Override
    public void initiateCall(Long translatorId, Long themeId, String callerUsername) {
        User caller     = userService.findByUsernameOrThrow(callerUsername);
        User translator = userService.findByIdOrThrow(translatorId);
        Theme theme     = themeService.findByIdOrThrow(themeId);

        if (!theme.isActive()) {
            throw new IllegalStateException("Theme is inactive");
        }

        Call call = Call.builder()
                .caller(caller)
                .recipient(translator)
                .theme(theme)
                .callStatus(CallStatus.CONNECT_NOT_SET)
                .build();
        call = callRepository.save(call);

        String callId = String.valueOf(call.getId());
        call.setChannelName(callId);
        call = callRepository.save(call);

        try {
            depositService.authorizeCallStartByTheme(caller.getId(), callId, themeId);
        } catch (RuntimeException e) {
            call.setCallStatus(CallStatus.MISSED);
            call.setEndCall(true);
            callRepository.save(call);
            log.warn("Call {} preauth failed for user {}: {}", callId, caller.getUsername(), e.getMessage());
            callRepository.delete(call);
            return;
        }

        activeCalls.put(callId, call);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .channelName(callId)
                .callerId(caller.getUsername())
                .callerName(fullName(caller))
                .recipientId(translator.getUsername())
                .recipientName(fullName(translator))
                .themeId(themeId)
                .themeName(theme.getName())
                .type("INCOMING_CALL")
                .status("PENDING")
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                translator.getUsername(),
                "/queue/call-notifications",
                notification
        );

        log.info("Call initiated: callId={}, from={} to={}", callId, caller.getUsername(), translator.getUsername());
    }

    @Override
    public void acceptCall(String callId, String translatorUsername) {
        Call call = getCallFromActiveOrDb(callId);
        if (call == null) {
            log.warn("Call not found: {}", callId);
            return;
        }

        if (!call.getRecipient().getUsername().equals(translatorUsername)) {
            log.warn("Translator {} attempted to accept call {} but is not the recipient",
                    translatorUsername, callId);
            return;
        }

        call.setCallStatus(CallStatus.SUCCESSFUL);
        call.setTranslatorHasJoined(true);

        callRepository.save(call);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .channelName(call.getChannelName())
                .type("CALL_ACCEPTED")
                .status("ACTIVE")
                .recipientId(call.getRecipient().getUsername())
                .recipientName(fullName(call.getRecipient()))
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                call.getCaller().getUsername(),
                "/queue/call-notifications",
                notification
        );

        log.info("Call accepted: callId={}, translator={}", callId, translatorUsername);
    }

    @Override
    public void rejectCall(String callId, String translatorUsername) {
        Call call = activeCalls.remove(callId);
        if (call == null) {
            call = getCallFromDb(callId);
        }

        if (call == null) {
            log.warn("Call not found: {}", callId);
            return;
        }

        if (!call.getRecipient().getUsername().equals(translatorUsername)) {
            log.warn("Translator {} attempted to reject call {} but is not the recipient",
                    translatorUsername, callId);
            return;
        }

        call.setCallStatus(CallStatus.MISSED);
        callRepository.save(call);

        debtorService.releasePreauthByCall(callId);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .channelName(call.getChannelName())
                .type("CALL_REJECTED")
                .status("REJECTED")
                .recipientId(call.getRecipient().getUsername())
                .recipientName(fullName(call.getRecipient()))
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                call.getCaller().getUsername(),
                "/queue/call-notifications",
                notification
        );

        log.info("Call rejected: callId={}, translator={}", callId, translatorUsername);
    }

    @Override
    public void endCall(String callId, String username) {
        Call call = activeCalls.remove(callId);
        if (call == null) {
            call = getCallFromDb(callId);
        }

        if (call == null) {
            log.warn("Call not found: {}", callId);
            return;
        }

        call.setEndCall(true);
        if (!call.getTranslatorHasJoined()) {
            call.setCallStatus(CallStatus.MISSED);
        } else {
            call.setCallStatus(CallStatus.SUCCESSFUL);
        }
        callRepository.save(call);

        debtorService.releasePreauthByCall(callId);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .channelName(call.getChannelName())
                .type("CALL_ENDED")
                .status("ENDED")
                .recipientId(call.getRecipient().getUsername())
                .recipientName(fullName(call.getRecipient()))
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                call.getCaller().getUsername(),
                "/queue/call-notifications",
                notification
        );

        messagingTemplate.convertAndSendToUser(
                call.getRecipient().getUsername(),
                "/queue/call-notifications",
                notification
        );

        log.info("Call ended: callId={}, endedBy={}", callId, username);
    }

    @Override
    public void handleCallSignaling(String callId, Object signalData, String username) {
        Call call = getCallFromActiveOrDb(callId);
        if (call == null) {
            log.warn("Call not found for signaling: {}", callId);
            return;
        }

        boolean isCaller = call.getCaller().getUsername().equals(username);
        boolean isRecipient = call.getRecipient().getUsername().equals(username);

        if (!isCaller && !isRecipient) {
            log.warn("User {} attempted to send signaling for call {} but is not a participant", username, callId);
            return;
        }

        String targetUser = isCaller
                ? call.getRecipient().getUsername()
                : call.getCaller().getUsername();

        messagingTemplate.convertAndSendToUser(
                targetUser,
                "/queue/webrtc-signals",
                signalData
        );

        log.debug("Signaling forwarded from {} to {} for call {}", username, targetUser, callId);
    }

    @Override
    public List<CallDto> getCallHistoryForUser(String username) {
        User user = userService.findByUsernameOrThrow(username);
        return callRepository.findByCaller_IdOrRecipient_Id(user.getId(), user.getId())
                .stream()
                .map(mapper::toDto)
                .toList();
    }



    private Call getCallFromActiveOrDb(String callId) {
        Call call = activeCalls.get(callId);
        if (call != null) {
            return call;
        }
        return getCallFromDb(callId);
    }


    private Call getCallFromDb(String callId) {
        try {
            Long id = Long.parseLong(callId);
            return callRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            log.warn("Invalid callId format: {}", callId);
            return null;
        }
    }

    private static String fullName(User u) {
        String fn = u.getFirstName() == null ? "" : u.getFirstName();
        String ln = u.getLastName()  == null ? "" : u.getLastName();
        String s = (fn + " " + ln).trim();
        return s.isEmpty() ? u.getUsername() : s;
    }
}