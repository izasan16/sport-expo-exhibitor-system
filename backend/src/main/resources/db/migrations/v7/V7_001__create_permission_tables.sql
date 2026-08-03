alter table __schema__.roles
    add column if not exists role_type varchar(20) not null default 'SYSTEM',
    add column if not exists enabled boolean not null default true,
    add column if not exists description varchar(500),
    add column if not exists created_by uuid,
    add column if not exists updated_by uuid;

create table if not exists __schema__.permissions (
    id uuid primary key,
    code varchar(200) not null unique,
    name varchar(160) not null,
    resource_type varchar(20) not null,
    module_key varchar(80) not null,
    parent_id uuid references __schema__.permissions(id),
    route_path varchar(255),
    action_key varchar(160),
    sort_order int not null default 0,
    enabled boolean not null default true,
    description varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.role_permissions (
    role_id uuid not null references __schema__.roles(id),
    permission_id uuid not null references __schema__.permissions(id),
    created_at timestamptz not null,
    created_by uuid,
    primary key (role_id, permission_id)
);

create index if not exists idx_permissions_parent_sort
    on __schema__.permissions(parent_id, sort_order);

create index if not exists idx_permissions_type_module
    on __schema__.permissions(resource_type, module_key);
