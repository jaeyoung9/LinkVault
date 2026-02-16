package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.InvitationCode;
import org.link.linkvault.entity.Role;

import java.time.LocalDateTime;

@Getter
@Builder
public class InvitationCodeResponseDto {

    private Long id;
    private String code;
    private String createdByUsername;
    private int maxUses;
    private int currentUses;
    private boolean active;
    private LocalDateTime expiresAt;
    private String note;
    private Role assignedRole;
    private LocalDateTime createdAt;
    private boolean usable;

    public static InvitationCodeResponseDto from(InvitationCode ic) {
        return InvitationCodeResponseDto.builder()
                .id(ic.getId())
                .code(ic.getCode())
                .createdByUsername(ic.getCreatedBy() != null ? ic.getCreatedBy().getUsername() : null)
                .maxUses(ic.getMaxUses())
                .currentUses(ic.getCurrentUses())
                .active(ic.isActive())
                .expiresAt(ic.getExpiresAt())
                .note(ic.getNote())
                .assignedRole(ic.getAssignedRole())
                .createdAt(ic.getCreatedAt())
                .usable(ic.isUsable())
                .build();
    }
}
