package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.PermissionResponseDto;
import org.link.linkvault.entity.Permission;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.RolePermission;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.PermissionRepository;
import org.link.linkvault.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogService auditLogService;

    public List<PermissionResponseDto> getPermissionsForRole(Role role) {
        Set<String> granted = rolePermissionRepository.findPermissionNamesByRole(role)
                .stream().collect(Collectors.toSet());

        return permissionRepository.findAll().stream()
                .map(p -> PermissionResponseDto.from(p, granted.contains(p.getName())))
                .collect(Collectors.toList());
    }

    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Transactional
    public void grantPermission(Role role, Long permissionId) {
        if (role == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot modify SUPER_ADMIN permissions");
        }
        if (rolePermissionRepository.existsByRoleAndPermissionId(role, permissionId)) {
            return;
        }
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
        rolePermissionRepository.save(new RolePermission(role, permission));
    }

    @Transactional
    public void revokePermission(Role role, Long permissionId) {
        if (role == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot modify SUPER_ADMIN permissions");
        }
        rolePermissionRepository.deleteByRoleAndPermissionId(role, permissionId);
    }

    @Transactional
    public void togglePermission(Role role, Long permissionId, boolean granted, String actorUsername) {
        if (granted) {
            grantPermission(role, permissionId);
        } else {
            revokePermission(role, permissionId);
        }
        auditLogService.log(actorUsername, AuditActionCodes.PERMISSION_TOGGLE, "Permission", permissionId,
                AuditDetailFormatter.format("role", String.valueOf(role), "granted", String.valueOf(granted)));
    }
}
