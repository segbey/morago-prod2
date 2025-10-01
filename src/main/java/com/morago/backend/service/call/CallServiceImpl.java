package com.morago.backend.service.call;

import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.tokens.CallNotificationMessage;
import com.morago.backend.entity.Call;
import com.morago.backend.entity.Theme;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.CallStatus;
import com.morago.backend.mapper.CallMapper;
import com.morago.backend.repository.CallRepository;
import com.morago.backend.repository.UserRepository;
import com.morago.backend.repository.ThemeRepository;
import com.morago.backend.exception.ResourceNotFoundException;
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
    private final UserRepository userRepository;
    private final ThemeRepository themeRepository;
    private final CallMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, Call> activeCalls = new ConcurrentHashMap<>();

    private <T> T findOrThrow(java.util.Optional<T> optional, String entityName, Long id) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(entityName + " not found with id " + id));
    }

    @Override
    public CallDto createCall(CallDto dto) {
        Call call = mapper.toEntity(dto);

        if (dto.getCallerId() != null) {
            call.setCaller(findOrThrow(userRepository.findById(dto.getCallerId()), "User", dto.getCallerId()));
        }
        if (dto.getRecipientId() != null) {
            call.setRecipient(findOrThrow(userRepository.findById(dto.getRecipientId()), "User", dto.getRecipientId()));
        }
        if (dto.getThemeId() != null) {
            call.setTheme(findOrThrow(themeRepository.findById(dto.getThemeId()), "Theme", dto.getThemeId()));
        }

        return mapper.toDto(callRepository.save(call));
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

        if (dto.getCallerId() != null) {
            call.setCaller(findOrThrow(userRepository.findById(dto.getCallerId()), "User", dto.getCallerId()));
        }
        if (dto.getRecipientId() != null) {
            call.setRecipient(findOrThrow(userRepository.findById(dto.getRecipientId()), "User", dto.getRecipientId()));
        }
        if (dto.getThemeId() != null) {
            call.setTheme(findOrThrow(themeRepository.findById(dto.getThemeId()), "Theme", dto.getThemeId()));
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
        User caller = userRepository.findByUsername(callerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Caller not found: " + callerUsername));
        User translator = findOrThrow(userRepository.findById(translatorId), "Translator", translatorId);
        Theme theme = findOrThrow(themeRepository.findById(themeId), "Theme", themeId);

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

        activeCalls.put(callId, call);

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .callerId(caller.getUsername())
                .callerName(caller.getFirstName() + " " + caller.getLastName())
                .recipientId(translator.getUsername())
                .recipientId(translator.getFirstName() + " " + translator.getLastName())
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
                .type("CALL_ACCEPTED")
                .status("ACTIVE")
                .translatorId(translatorUsername)
                .translatorId(call.getRecipient().getFirstName() + " " + call.getRecipient().getLastName())
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

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .type("CALL_REJECTED")
                .status("REJECTED")
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

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .type("CALL_ENDED")
                .status("ENDED")
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

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
}