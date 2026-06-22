update __schema__.roles
set role_type = case
        when btrim(coalesce(role_type, '')) = '' then 'SYSTEM'
        else role_type
    end,
    description = coalesce(description, case code
        when 'SYSTEM_ADMIN' then '系统默认超级管理员角色'
        when 'ORGANIZER_ADMIN' then '系统默认主办方管理员角色'
        when 'SALES' then '系统默认销售角色'
        when 'FINANCE' then '系统默认财务角色'
        when 'SERVICE_STAFF' then '系统默认服务人员角色'
        when 'EXHIBITOR' then '系统默认展商角色'
        else description
    end),
    updated_at = case
        when btrim(coalesce(role_type, '')) = '' or description is null then now()
        else updated_at
    end
where code in ('SYSTEM_ADMIN', 'ORGANIZER_ADMIN', 'SALES', 'FINANCE', 'SERVICE_STAFF', 'EXHIBITOR')
  and (btrim(coalesce(role_type, '')) = '' or description is null);

insert into __schema__.role_permissions (role_id, permission_id, created_at, created_by)
select r.id, p.id, now(), null
from __schema__.roles r
join __schema__.permissions p on 1 = 1
where r.code = 'SYSTEM_ADMIN'
on conflict do nothing;

insert into __schema__.role_permissions (role_id, permission_id, created_at, created_by)
select r.id, p.id, now(), null
from __schema__.roles r
join __schema__.permissions p on p.code in (
    'menu.admin.users',
    'page.admin.users.list',
    'menu.admin.roles',
    'page.admin.roles.list',
    'menu.admin.permissions',
    'page.admin.permissions.list',
    'menu.admin.role-permissions',
    'page.admin.role-permissions.matrix',
    'action.admin.users.create',
    'action.admin.users.edit',
    'action.admin.users.enable',
    'action.admin.users.disable',
    'action.admin.users.reset-password',
    'action.admin.users.assign-roles',
    'action.admin.users.set-default-role',
    'action.admin.roles.create',
    'action.admin.roles.edit',
    'action.admin.roles.copy',
    'action.admin.roles.toggle-status',
    'action.admin.roles.assign-permissions',
    'action.admin.permissions.create',
    'action.admin.permissions.edit',
    'action.admin.permissions.toggle-status'
)
where r.code = 'ORGANIZER_ADMIN'
on conflict do nothing;
