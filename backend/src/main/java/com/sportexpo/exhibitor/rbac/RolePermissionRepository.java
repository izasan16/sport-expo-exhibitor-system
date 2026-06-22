package com.sportexpo.exhibitor.rbac;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    @Query("""
            select rolePermission
            from RolePermission rolePermission
            join fetch rolePermission.permission permission
            where rolePermission.role.id = :roleId
            order by permission.sortOrder asc, permission.code asc
            """)
    List<RolePermission> findByRoleId(@Param("roleId") UUID roleId);
}
