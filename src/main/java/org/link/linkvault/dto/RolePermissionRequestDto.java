package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.link.linkvault.entity.Role;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionRequestDto {

    @NotNull(message = "Role is required")
    private Role role;

    @NotNull(message = "Permission ID is required")
    private Long permissionId;

    private boolean granted;
}
