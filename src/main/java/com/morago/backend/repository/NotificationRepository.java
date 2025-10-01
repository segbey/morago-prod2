package com.morago.backend.repository;

import com.morago.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find notifications by user ID, ordered by date (newest first)
    List<Notification> findByUserIdOrderByDateTimeDesc(Long userId);

    // Find notifications by user ID with pagination
    Page<Notification> findByUserIdOrderByDateTimeDesc(Long userId, Pageable pageable);

    // Find unread notifications for a user
    List<Notification> findByUserIdAndIsReadFalseOrderByDateTimeDesc(Long userId);

    // Count unread notifications for a user
    long countByUserIdAndIsReadFalse(Long userId);

    // Delete all notifications for a user
    void deleteByUserId(Long userId);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    // Mark specific notification as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId);
}
