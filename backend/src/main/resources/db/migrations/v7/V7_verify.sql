select count(*) as permission_count
from __schema__.permissions;

select count(*) as role_permission_count
from __schema__.role_permissions;

select
    (select count(*)
     from __schema__.permissions) as permission_count,
    (select count(*)
     from __schema__.role_permissions rp
     join __schema__.roles r on r.id = rp.role_id
     where r.code = 'SYSTEM_ADMIN') as system_admin_permission_count,
    (select count(*)
     from __schema__.role_permissions rp
     join __schema__.roles r on r.id = rp.role_id
     where r.code = 'ORGANIZER_ADMIN') as organizer_admin_permission_count;

select code, role_type, enabled, description
from __schema__.roles
where code in (
    'SYSTEM_ADMIN',
    'ORGANIZER_ADMIN',
    'SALES',
    'FINANCE',
    'SERVICE_STAFF',
    'EXHIBITOR'
)
order by code;

select code
from __schema__.permissions
where code in (
    'menu.admin.users',
    'page.admin.users.list',
    'menu.admin.roles',
    'page.admin.permissions.list',
    'page.admin.role-permissions.matrix'
)
order by code;

select p.code
from __schema__.role_permissions rp
join __schema__.roles r on r.id = rp.role_id
join __schema__.permissions p on p.id = rp.permission_id
where r.code = 'ORGANIZER_ADMIN'
  and p.code in (
      'menu.admin.users',
      'page.admin.users.list',
      'menu.admin.roles',
      'page.admin.permissions.list',
      'page.admin.role-permissions.matrix',
      'action.admin.roles.assign-permissions'
  )
order by p.code;
