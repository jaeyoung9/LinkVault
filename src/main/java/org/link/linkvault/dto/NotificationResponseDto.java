package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Notification;
import org.link.linkvault.entity.NotificationType;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {

    private Long id;
    private String sourceUsername;
    private NotificationType type;
    private String message;
    private Long relatedBookmarkId;
    private Long relatedCommentId;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .sourceUsername(notification.getSourceUser() != null ? notification.getSourceUser().getUsername() : "System")
                .type(notification.getType())
                .message(notification.getMessage())
                .relatedBookmarkId(notification.getRelatedBookmarkId())
                .relatedCommentId(notification.getRelatedCommentId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
