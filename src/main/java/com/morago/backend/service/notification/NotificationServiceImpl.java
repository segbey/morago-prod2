package com.morago.backend.service.notification;

import com.morago.backend.dto.NotificationDto;
import com.morago.backend.dto.tokens.NotificationMessage;
import com.morago.backend.entity.Notification;
import com.morago.backend.entity.User;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.mapper.NotificationMapper;
import com.morago.backend.repository.NotificationRepository;
import com.morago.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationDto create(NotificationDto dto) {
        Notification notification = notificationMapper.toEntity(dto);
        notification.setDateTime(LocalDateTime.now());
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: id={}, userId={}, title={}",
                saved.getId(), saved.getUser().getId(), saved.getTitle());

        return notificationMapper.toDto(saved);
    }

    @Override
    public NotificationDto update(Long id, NotificationDto dto) {
        Notification existing = getEntityById(id);
        existing.setTitle(dto.getTitle());
        existing.setText(dto.getText());

        Notification updated = notificationRepository.save(existing);
        log.info("Updated notification: id={}", id);

        return notificationMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto getById(Long id) {
        return notificationMapper.toDto(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        Notification notification = getEntityById(id);
        notificationRepository.delete(notification);
        log.info("Deleted notification: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByUserId(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByDateTimeDesc(userId);
        log.info("Retrieved {} notifications for userId={}", notifications.size(), userId);

        return notifications.stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void clearNotificationsForUser(Long userId) {
        notificationRepository.deleteByUserId(userId);
        log.info("Cleared all notifications for userId={}", userId);
    }

    public void notifyCallEvent(Long userId, String title, String text, String type) {
        try {
            User user = userService.findByIdOrThrow(userId);

            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .text(text)
                    .dateTime(LocalDateTime.now())
                    .isRead(false)
                    .build();

            notification = notificationRepository.save(notification);
            log.info("Created call notification: userId={}, type={}, title={}", userId, type, title);

            NotificationMessage wsMessage = NotificationMessage.builder()
                    .id(String.valueOf(notification.getId()))
                    .title(title)
                    .text(text)
                    .recipientId(user.getUsername())
                    .sender("System")
                    .type(type)
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    wsMessage
            );

            log.info("Sent WebSocket notification to user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to create call notification for userId={}: {}", userId, e.getMessage(), e);
        }
    }

    public void notifyIncomingCall(Long recipientId, String callerName, Long themeId, String themeName, String callId) {
        String title = "Incoming call";
        String text = String.format("Call from %s about '%s'", callerName, themeName);
        notifyCallEvent(recipientId, title, text, "INCOMING_CALL");
    }

    public void notifyCallAccepted(Long callerId, String translatorName, String callId) {
        String title = "Call accepted";
        String text = String.format("Translator %s accepted your call", translatorName);
        notifyCallEvent(callerId, title, text, "CALL_ACCEPTED");
    }

    public void notifyCallRejected(Long callerId, String translatorName, String callId) {
        String title = "Call rejected";
        String text = String.format("Translator %s rejected your call", translatorName);
        notifyCallEvent(callerId, title, text, "CALL_REJECTED");
    }

    public void notifyCallEnded(Long userId, int durationMinutes, java.math.BigDecimal charge, boolean wasSuccessful) {
        String title = "Call ended";
        String text = wasSuccessful ?
                String.format("Duration: %d min. Cost: %s ₩", durationMinutes, charge) :
                "The call was interrupted";
        notifyCallEvent(userId, title, text, "CALL_ENDED");
    }

    public void notifyLowBalance(Long userId, java.math.BigDecimal currentBalance, java.math.BigDecimal requiredBalance) {
        String title = "Insufficient funds";
        String text = String.format("Balance: %s ₩. Required: %s ₩", currentBalance, requiredBalance);
        notifyCallEvent(userId, title, text, "LOW_BALANCE");
    }

    public void notifyPaymentProcessed(Long userId, java.math.BigDecimal amount, String description) {
        String title = "Payment processed";
        String text = String.format("%s: %s ₩", description, amount);
        notifyCallEvent(userId, title, text, "PAYMENT_PROCESSED");
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
        log.info("Marked notification as read: id={}", notificationId);
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked all notifications as read for userId={}", userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        log.debug("Unread notifications for userId={}: {}", userId, count);
        return count;
    }

    private Notification getEntityById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
    }
}
