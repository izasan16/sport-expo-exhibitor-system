create table if not exists __schema__.events (
    id uuid primary key,
    name varchar(200) not null,
    year int not null,
    status varchar(40) not null,
    registration_start_at timestamptz,
    registration_end_at timestamptz,
    payment_deadline_at timestamptz,
    selection_start_at timestamptz,
    selection_end_at timestamptz,
    service_start_at timestamptz,
    service_end_at timestamptz,
    default_lock_minutes int not null default 1440,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.pavilions (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    name varchar(200) not null,
    code varchar(80) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, code)
);

create table if not exists __schema__.zones (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    pavilion_id uuid not null references __schema__.pavilions(id),
    name varchar(200) not null,
    code varchar(80) not null,
    category_code varchar(80) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, code)
);

create table if not exists __schema__.booths (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    pavilion_id uuid references __schema__.pavilions(id),
    zone_id uuid references __schema__.zones(id),
    booth_no varchar(80) not null,
    area_sqm numeric(14,2) not null,
    booth_type varchar(60) not null,
    status varchar(40) not null,
    x numeric(14,2),
    y numeric(14,2),
    width numeric(14,2),
    height numeric(14,2),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, booth_no)
);

create table if not exists __schema__.area_tiers (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    zone_id uuid references __schema__.zones(id),
    area_sqm numeric(14,2) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.price_rules (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    zone_id uuid references __schema__.zones(id),
    booth_type varchar(60) not null,
    unit_price numeric(14,2) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.contracts (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    current_version_id uuid,
    contract_no varchar(100) not null,
    status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(event_id, contract_no)
);

create table if not exists __schema__.contract_versions (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    contract_id uuid not null references __schema__.contracts(id),
    version_no int not null,
    reason varchar(200) not null,
    content_snapshot jsonb not null,
    amount numeric(14,2) not null,
    area_sqm numeric(14,2) not null,
    sign_status varchar(40) not null,
    signer_id uuid,
    signed_at timestamptz,
    file_id uuid,
    created_at timestamptz not null,
    created_by uuid,
    unique(contract_id, version_no)
);

create table if not exists __schema__.participation_orders (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    exhibitor_id uuid not null,
    registration_id uuid,
    zone_id uuid references __schema__.zones(id),
    area_tier_id uuid references __schema__.area_tiers(id),
    status varchar(40) not null,
    area_fee numeric(14,2) not null,
    service_fee numeric(14,2) not null default 0,
    discount_amount numeric(14,2) not null default 0,
    adjustment_amount numeric(14,2) not null default 0,
    total_amount numeric(14,2) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.payments (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    amount numeric(14,2) not null,
    status varchar(40) not null,
    method varchar(40) not null,
    confirmed_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.refunds (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    amount numeric(14,2) not null,
    status varchar(40) not null,
    reason varchar(500) not null,
    approved_by uuid,
    processed_by uuid,
    processed_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.refund_payment_items (
    id uuid primary key,
    refund_id uuid not null references __schema__.refunds(id),
    payment_id uuid not null references __schema__.payments(id),
    amount numeric(14,2) not null,
    created_at timestamptz not null,
    created_by uuid
);

create table if not exists __schema__.selection_eligibilities (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    status varchar(40) not null,
    pause_reason varchar(80),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(order_id)
);

create table if not exists __schema__.booth_selections (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    exhibitor_id uuid not null,
    status varchar(40) not null,
    total_area_sqm numeric(14,2) not null,
    lock_started_at timestamptz,
    lock_expires_at timestamptz,
    helper_user_id uuid,
    reviewer_user_id uuid,
    reviewed_at timestamptz,
    review_comment varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.booth_locks (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    selection_id uuid not null references __schema__.booth_selections(id),
    booth_id uuid not null references __schema__.booths(id),
    status varchar(40) not null,
    lock_expires_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.booth_release_tasks (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
    selection_id uuid references __schema__.booth_selections(id),
    status varchar(40) not null,
    reason varchar(500) not null,
    handled_by uuid,
    handled_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.audit_logs (
    id uuid primary key,
    event_id uuid,
    actor_id uuid,
    action varchar(100) not null,
    object_type varchar(100) not null,
    object_id uuid,
    old_status varchar(40),
    new_status varchar(40),
    reason varchar(500),
    request_source varchar(80),
    created_at timestamptz not null
);

create index if not exists idx_booths_event_status on __schema__.booths(event_id, status);
create index if not exists idx_payments_order_status on __schema__.payments(order_id, status);
create index if not exists idx_refunds_order_status on __schema__.refunds(order_id, status);
create index if not exists idx_booth_locks_event_booth_status on __schema__.booth_locks(event_id, booth_id, status);
create index if not exists idx_release_tasks_event_status on __schema__.booth_release_tasks(event_id, status);
create index if not exists idx_audit_logs_object on __schema__.audit_logs(object_type, object_id);
