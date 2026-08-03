insert into __schema__.roles (id, code, name, created_at, updated_at)
values
    ('00000000-0000-0000-0000-000000000101', 'SYSTEM_ADMIN', '系统管理员', now(), now()),
    ('00000000-0000-0000-0000-000000000102', 'ORGANIZER_ADMIN', '主办方管理员', now(), now()),
    ('00000000-0000-0000-0000-000000000103', 'SALES', '销售', now(), now()),
    ('00000000-0000-0000-0000-000000000104', 'FINANCE', '财务', now(), now()),
    ('00000000-0000-0000-0000-000000000105', 'SERVICE_STAFF', '服务人员', now(), now()),
    ('00000000-0000-0000-0000-000000000106', 'EXHIBITOR', '展商', now(), now())
on conflict (code) do nothing;
