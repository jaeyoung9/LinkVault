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
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private LocalDateTime timestamp;

    public static AuditLogResponseDto from(AuditLog log) {
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .username(log.getUser() != null ? log.getUser().getUsername() : null)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}
