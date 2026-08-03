create table if not exists __schema__.roles (
    id uuid primary key,
    code varchar(80) not null unique,
    name varchar(120) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists __schema__.users (
    id uuid primary key,
    username varchar(120) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(120) not null,
    phone varchar(40),
    email varchar(120),
    user_type varchar(40) not null,
    exhibitor_id uuid,
    status varchar(40) not null,
    default_role varchar(80),
    failed_login_count int not null default 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    jwt_version int not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.user_roles (
    user_id uuid not null references __schema__.users(id),
    role_id uuid not null references __schema__.roles(id),
    created_at timestamptz not null,
    primary key (user_id, role_id)
);
