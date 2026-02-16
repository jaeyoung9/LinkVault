package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Announcement;
import org.link.linkvault.entity.AnnouncementPriority;
import org.link.linkvault.entity.AnnouncementStatus;
import org.link.linkvault.entity.Role;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnnouncementResponseDto {
    private Long id;
    private String title;
    private String content;
    private AnnouncementPriority priority;
    private AnnouncementStatus status;
    private Role targetRole;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean pinned;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean read;
    private boolean acknowledged;

    public static AnnouncementResponseDto from(Announcement a) {
        return from(a, false, false);
    }

    public static AnnouncementResponseDto from(Announcement a, boolean isRead, boolean isAcknowledged) {
        return AnnouncementResponseDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .priority(a.getPriority())
                .status(a.getStatus())
                .targetRole(a.getTargetRole())
                .startAt(a.getStartAt())
                .endAt(a.getEndAt())
                .pinned(a.isPinned())
                .createdByUsername(a.getCreatedBy() != null ? a.getCreatedBy().getUsername() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .read(isRead)
                .acknowledged(isAcknowledged)
                .build();
    }
}
