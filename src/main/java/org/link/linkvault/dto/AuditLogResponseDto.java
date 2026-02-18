package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.AuditLog;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponseDto {

    private Long id;
    private String username;
    private String actorLabel;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private LocalDateTime timestamp;

    public static AuditLogResponseDto from(AuditLog log) {
        String username = log.getUser() != null ? log.getUser().getUsername() : null;
        String actorLabel = username != null ? username :
                (log.getActorUsername() != null ? log.getActorUsername() + " (deleted)" : "system");
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .username(username)
                .actorLabel(actorLabel)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}
