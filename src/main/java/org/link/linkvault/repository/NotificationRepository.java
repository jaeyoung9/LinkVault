package org.link.linkvault.repository;

import org.link.linkvault.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value = "SELECT n FROM Notification n LEFT JOIN FETCH n.sourceUser " +
            "WHERE n.recipient.id = :recipientId " +
            "AND (n.read = false OR n.createdAt > :readCutoff) " +
            "ORDER BY n.createdAt DESC",
           countQuery = "SELECT COUNT(n) FROM Notification n " +
                   "WHERE n.recipient.id = :recipientId " +
                   "AND (n.read = false OR n.createdAt > :readCutoff)")
    Page<Notification> findVisibleByRecipientId(@Param("recipientId") Long recipientId,
                                                @Param("readCutoff") LocalDateTime readCutoff,
                                                Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    void markAllAsRead(@Param("recipientId") Long recipientId);

    void deleteByRecipientId(Long recipientId);

    void deleteBySourceUserId(Long sourceUserId);

    boolean existsBySourceUserIdAndRecipientIdAndTypeAndRelatedCommentId(
            Long sourceUserId, Long recipientId, org.link.linkvault.entity.NotificationType type, Long relatedCommentId);
}
