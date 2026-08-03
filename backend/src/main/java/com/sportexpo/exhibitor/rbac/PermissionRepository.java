package com.sportexpo.exhibitor.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    boolean existsByCode(String code);

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByOrderBySortOrderAscCodeAsc();

    @Query("""
            select distinct permission
            from RolePermission rolePermission
            join rolePermission.role role
            join rolePermission.permission permission
            where role.code in :roleCodes
              and permission.enabled = true
            order by permission.sortOrder asc, permission.code asc
            """)
    List<Permission> findEnabledPermissionsByRoleCodes(@Param("roleCodes") Collection<String> roleCodes);
}
