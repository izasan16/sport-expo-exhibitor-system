-- Default password for local demo accounts: demo123
-- BCrypt cost: 10
-- Hash generated once during implementation and intentionally not overwritten by ON CONFLICT.
insert into __schema__.users (
    id, username, password_hash, display_name, phone, email, user_type, exhibitor_id, status, default_role,
    failed_login_count, jwt_version, created_at, updated_at, created_by, updated_by
)
values
    (
        '00000000-0000-0000-0000-000000000201', 'admin',
        '$2b$10$Xq2VGIvlkBrDmwGUNPhgaut9mofpvTOMkBuuTHzHBP01vEQ0/a2HG',
        '系统管理员', null, 'admin@example.com', 'ADMIN', null, 'ACTIVE', 'SYSTEM_ADMIN',
        0, 0, now(), now(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'
    ),
    (
        '00000000-0000-0000-0000-000000000202', 'organizer',
        '$2b$10$Xq2VGIvlkBrDmwGUNPhgaut9mofpvTOMkBuuTHzHBP01vEQ0/a2HG',
        '主办方管理员', null, 'organizer@example.com', 'ADMIN', null, 'ACTIVE', 'ORGANIZER_ADMIN',
        0, 0, now(), now(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'
    ),
    (
        '00000000-0000-0000-0000-000000000203', 'sales01',
        '$2b$10$Xq2VGIvlkBrDmwGUNPhgaut9mofpvTOMkBuuTHzHBP01vEQ0/a2HG',
        '销售一号', null, 'sales01@example.com', 'STAFF', null, 'ACTIVE', 'SALES',
        0, 0, now(), now(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'
    ),
    (
        '00000000-0000-0000-0000-000000000204', 'finance01',
        '$2b$10$Xq2VGIvlkBrDmwGUNPhgaut9mofpvTOMkBuuTHzHBP01vEQ0/a2HG',
        '财务一号', null, 'finance01@example.com', 'STAFF', null, 'ACTIVE', 'FINANCE',
        0, 0, now(), now(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'
    ),
    (
        '00000000-0000-0000-0000-000000000205', 'service01',
        '$2b$10$Xq2VGIvlkBrDmwGUNPhgaut9mofpvTOMkBuuTHzHBP01vEQ0/a2HG',
        '服务一号', null, 'service01@example.com', 'STAFF', null, 'ACTIVE', 'SERVICE_STAFF',
        0, 0, now(), now(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'
    ),
    (
        '00000000-0000-0000-0000-000000000206', 'exhibitor01',
        '$2b$10$Xq2VGIvlkBrDmwGUNPhgaut9mofpvTOMkBuuTHzHBP01vEQ0/a2HG',
        '演示展商账号', null, 'exhibitor01@example.com', 'EXHIBITOR', '00000000-0000-0000-0000-000000000001', 'ACTIVE', 'EXHIBITOR',
        0, 0, now(), now(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'
    )
on conflict (username) do update set
    display_name = excluded.display_name,
    phone = excluded.phone,
    email = excluded.email,
    user_type = excluded.user_type,
    exhibitor_id = excluded.exhibitor_id,
    status = excluded.status,
    default_role = excluded.default_role,
    updated_at = now(),
    updated_by = excluded.updated_by;

insert into __schema__.user_roles (user_id, role_id, created_at)
select u.id, r.id, now()
from __schema__.users u
join __schema__.roles r on r.code = u.default_role
where u.username in ('admin', 'organizer', 'sales01', 'finance01', 'service01', 'exhibitor01')
on conflict (user_id, role_id) do nothing;
