create table if not exists __schema__.exhibitor_sales_assignments (
    id uuid primary key,
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    sales_user_id uuid not null references __schema__.users(id),
    assignment_type varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique (exhibitor_id, sales_user_id, assignment_type)
);

create table if not exists __schema__.sales_follow_ups (
    id uuid primary key,
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    sales_user_id uuid not null references __schema__.users(id),
    method varchar(40) not null,
    content varchar(1000) not null,
    next_follow_up_at timestamptz,
    status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);
