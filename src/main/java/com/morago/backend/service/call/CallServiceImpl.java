package com.morago.backend.service.call;

import com.morago.backend.config.utils.ThemePriceUtil;
import com.morago.backend.dto.call.CallDto;
import com.morago.backend.dto.tokens.CallNotificationMessage;
import com.morago.backend.entity.Call;
import com.morago.backend.entity.Money;
import com.morago.backend.entity.Theme;
import com.morago.backend.entity.User;
import com.morago.backend.entity.enumFiles.CallStatus;
import com.morago.backend.event.CallEndedEvent;
import com.morago.backend.mapper.CallMapper;
import com.morago.backend.repository.CallRepository;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.service.debtor.DebtorService;
import com.morago.backend.service.deposit.DepositService;
import com.morago.backend.service.pricing.PricingService;
import com.morago.backend.service.theme.ThemeService;
import com.morago.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
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
    private final PricingService pricing;
    private final ApplicationEventPublisher publisher;

    private final Map<String, CallSessionData> activeCalls = new ConcurrentHashMap<>();

    private static class CallSessionData {
        Call call;
        LocalDateTime startTime;
        LocalDateTime acceptedTime;
        boolean translatorJoined;

        CallSessionData(Call call) {
            this.call = call;
            this.startTime = LocalDateTime.now();
            this.translatorJoined = false;
        }
    }

    @Override
    public CallDto getCallById(Long id) {
        return mapper.toDto(callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id " + id)));
    }

    @Override
    public List<CallDto> getAllCalls() {
        return callRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public CallDto updateCall(Long id, CallDto dto) {
        Call call = callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id " + id));

        call.setDuration(dto.getDuration());
        call.setStatus(dto.isStatus());
        call.setSumDecimal(Money.s2(dto.getSumDecimal()));
        call.setCommission(Money.s2(dto.getCommission()));
        call.setTranslatorHasJoined(dto.isTranslatorHasJoined());
        call.setUserHasRated(dto.isUserHasRated());
        call.setCallStatus(dto.getCallStatus());
        call.setEndCall(dto.isEndCall());
        call.setChannelName(dto.getChannelName());

        if (dto.getCallerId() != null && (call.getCaller() == null ||
                !dto.getCallerId().equals(call.getCaller().getId()))) {
            call.setCaller(userService.findByIdOrThrow(dto.getCallerId()));
        }
        if (dto.getRecipientId() != null && (call.getRecipient() == null ||
                !dto.getRecipientId().equals(call.getRecipient().getId()))) {
            call.setRecipient(userService.findByIdOrThrow(dto.getRecipientId()));
        }
        if (dto.getThemeId() != null && (call.getTheme() == null ||
                !dto.getThemeId().equals(call.getTheme().getId()))) {
            Theme theme = themeService.findByIdOrThrow(dto.getThemeId());
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
    public CallDto initiateCall(Long translatorId, Long themeId, String callerUsername) {
        log.info("=== INITIATE CALL SERVICE ===");
        log.info("translatorId={}, themeId={}, caller={}", translatorId, themeId, callerUsername);

        User caller = userService.findByUsernameOrThrow(callerUsername);
        User translator = userService.findByIdOrThrow(translatorId);

        if (themeId == null) {
            throw new IllegalArgumentException("themeId is required");
        }

        Theme theme = themeService.findByIdOrThrow(themeId);
        if (!theme.isActive()) {
            throw new IllegalStateException("Theme is inactive");
        }

        Call call = Call.builder()
                .caller(caller)
                .recipient(translator)
                .theme(theme)
                .callStatus(CallStatus.CONNECT_NOT_SET)
                .duration(0)
                .sumDecimal(BigDecimal.ZERO)
                .commission(ThemePriceUtil.perMinute(theme))
                .translatorHasJoined(false)
                .userHasRated(false)
                .endCall(false)
                .status(false)
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
            throw e;
        }

        activeCalls.put(callId, new CallSessionData(call));

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

        log.info("Call initiated: callId={}, from={} to={}, themeId={}, commission={}",
                callId, caller.getUsername(), translator.getUsername(), themeId, call.getCommission());

        return mapper.toDto(call);
    }

    @Override
    public void acceptCall(String callId, String translatorUsername) {
        log.info("=== ACCEPT CALL ===");
        log.info("callId={}, translator={}", callId, translatorUsername);

        CallSessionData session = activeCalls.get(callId);
        Call call = session != null ? session.call : getCallFromDb(callId);

        if (call == null) {
            log.warn("Call not found: {}", callId);
            return;
        }

        if (!call.getRecipient().getUsername().equals(translatorUsername)) {
            log.warn("Unauthorized accept attempt by {}", translatorUsername);
            return;
        }

        call.setCallStatus(CallStatus.SUCCESSFUL);
        call.setTranslatorHasJoined(true);
        call.setStatus(true);

        if (session == null) {
            session = new CallSessionData(call);
            activeCalls.put(callId, session);
            log.info("Recreated session for call {}", callId);
        }
        session.acceptedTime = LocalDateTime.now();
        session.translatorJoined = true;

        call = callRepository.save(call);
        log.info("Call accepted: callId={}, status={}, translatorJoined={}",
                callId, call.getCallStatus(), call.getTranslatorHasJoined());

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
    }

    @Override
    public void rejectCall(String callId, String translatorUsername) {
        log.info("=== REJECT CALL ===");
        log.info("callId={}, translator={}", callId, translatorUsername);

        CallSessionData session = activeCalls.remove(callId);
        Call call = session != null ? session.call : getCallFromDb(callId);

        if (call == null) {
            log.warn("Call not found: {}", callId);
            return;
        }

        if (!call.getRecipient().getUsername().equals(translatorUsername)) {
            log.warn("Unauthorized reject attempt by {}", translatorUsername);
            return;
        }

        call.setCallStatus(CallStatus.MISSED);
        call.setEndCall(true);
        call.setStatus(false);
        call.setTranslatorHasJoined(false);
        call.setDuration(0);
        call.setSumDecimal(BigDecimal.ZERO);

        call = callRepository.save(call);
        log.info("Call rejected: callId={}, status={}", callId, call.getCallStatus());

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
    }

    @Override
    public void endCall(String callId, String username) {
        log.info("=== END CALL ===");
        log.info("callId={}, endedBy={}", callId, username);

        CallSessionData session = activeCalls.remove(callId);
        Call call = session != null ? session.call : getCallFromDb(callId);

        if (call == null) {
            log.warn("Call not found: {}", callId);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        int durationMinutes = 0;
        BigDecimal totalCharge = BigDecimal.ZERO;

        if (call.getTranslatorHasJoined() && session != null && session.acceptedTime != null) {
            Duration duration = Duration.between(session.acceptedTime, endTime);
            long seconds = duration.getSeconds();
            durationMinutes = (int) Math.ceil(seconds / 60.0);
            BigDecimal commission = call.getCommission() != null ?
                    call.getCommission() : ThemePriceUtil.perMinute(call.getTheme());
            totalCharge = commission.multiply(BigDecimal.valueOf(durationMinutes));
            totalCharge = Money.s2(totalCharge);

            log.info("Call duration calculated: {} seconds = {} minutes, charge: {} (rate: {})",
                    seconds, durationMinutes, totalCharge, commission);
        }

        call.setEndCall(true);
        call.setStatus(false);
        call.setDuration(durationMinutes);
        call.setSumDecimal(totalCharge);

        if (!call.getTranslatorHasJoined()) {
            call.setCallStatus(CallStatus.MISSED);
        } else {
            call.setCallStatus(CallStatus.SUCCESSFUL);
        }

        call = callRepository.save(call);

        log.info("Call ended: callId={}, status={}, duration={}min, charge={}, translatorJoined={}",
                callId, call.getCallStatus(), durationMinutes, totalCharge, call.getTranslatorHasJoined());

        if (call.getTranslatorHasJoined() && totalCharge.compareTo(BigDecimal.ZERO) > 0) {
            try {
                Long clientId = call.getCaller().getId();
                Long interpreterId = call.getRecipient().getId();
                log.info("Publishing CallEndedEvent: clientId={}, interpreterId={}, callId={}, amount={}",
                        clientId, interpreterId, callId, totalCharge);
                publisher.publishEvent(new CallEndedEvent(clientId, interpreterId, callId, totalCharge));
            } catch (Exception e) {
                log.error("Failed to process payment for call {}: {}", callId, e.getMessage(), e);
            }
        } else {
            debtorService.releasePreauthByCall(callId);
        }

        CallNotificationMessage notification = CallNotificationMessage.builder()
                .callId(callId)
                .channelName(call.getChannelName())
                .type("CALL_ENDED")
                .status("ENDED")
                .recipientId(call.getRecipient().getUsername())
                .recipientName(fullName(call.getRecipient()))
                .timestamp(endTime)
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
    }

    @Override
    public void handleCallSignaling(String callId, Object signalData, String username) {
        Call call = activeCalls.containsKey(callId) ?
                activeCalls.get(callId).call : getCallFromDb(callId);
        if (call == null) return;

        boolean isCaller = call.getCaller().getUsername().equals(username);
        boolean isRecipient = call.getRecipient().getUsername().equals(username);
        if (!isCaller && !isRecipient) return;

        String targetUser = isCaller ?
                call.getRecipient().getUsername() : call.getCaller().getUsername();

        messagingTemplate.convertAndSendToUser(targetUser, "/queue/webrtc-signals", signalData);
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
        String ln = u.getLastName() == null ? "" : u.getLastName();
        String s = (fn + " " + ln).trim();
        return s.isEmpty() ? u.getUsername() : s;
    }
}