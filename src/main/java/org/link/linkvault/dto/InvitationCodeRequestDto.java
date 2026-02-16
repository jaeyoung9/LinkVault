package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.link.linkvault.entity.Role;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationCodeRequestDto {

    private int maxUses = 1;
    private Integer expiresInHours;
    private String note;
    private Role assignedRole;
}
