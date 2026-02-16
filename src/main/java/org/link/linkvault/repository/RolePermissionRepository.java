package org.link.linkvault.repository;

import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("SELECT rp.permission.name FROM RolePermission rp WHERE rp.role = :role")
    List<String> findPermissionNamesByRole(@Param("role") Role role);

    boolean existsByRoleAndPermissionId(Role role, Long permissionId);

    List<RolePermission> findByRole(Role role);

    void deleteByRoleAndPermissionId(Role role, Long permissionId);
}
