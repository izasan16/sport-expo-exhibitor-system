create table if not exists __schema__.service_types (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    name varchar(160) not null,
    category varchar(40) not null,
    target_scope varchar(40) not null,
    open_conditions text,
    charge_mode varchar(40) not null,
    fixed_price numeric(14,2),
    requires_review boolean not null default true,
    deadline_at timestamptz,
    enabled boolean not null default true,
    sort_order int not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.service_fields (
    id uuid primary key,
    service_type_id uuid not null references __schema__.service_types(id),
    code varchar(100) not null,
    label varchar(160) not null,
    field_type varchar(40) not null,
    required boolean not null default false,
    placeholder varchar(300),
    default_value text,
    options_json text,
    validation_json text,
    enabled boolean not null default true,
    sort_order int not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (service_type_id, code)
);

create table if not exists __schema__.service_applications (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    service_type_id uuid not null references __schema__.service_types(id),
    order_id uuid references __schema__.participation_orders(id),
    status varchar(40) not null,
    reviewer_user_id uuid references __schema__.users(id),
    reviewed_at timestamptz,
    review_comment varchar(500),
    supplement_reason varchar(500),
    exhibitor_remark varchar(500),
    submitted_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.service_application_booths (
    id uuid primary key,
    application_id uuid not null references __schema__.service_applications(id),
    booth_id uuid not null references __schema__.booths(id),
    created_at timestamptz not null,
    unique (application_id, booth_id)
);

create table if not exists __schema__.service_application_values (
    id uuid primary key,
    application_id uuid not null references __schema__.service_applications(id),
    field_code varchar(100) not null,
    value_text text,
    value_json jsonb,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (application_id, field_code)
);

create table if not exists __schema__.service_application_attachments (
    id uuid primary key,
    application_id uuid not null references __schema__.service_applications(id),
    field_code varchar(100) not null,
    file_id varchar(160) not null,
    file_name varchar(255) not null,
    content_type varchar(120),
    file_size bigint,
    created_at timestamptz not null,
    created_by uuid
);

create table if not exists __schema__.service_orders (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    service_application_id uuid not null references __schema__.service_applications(id),
    service_type_id uuid not null references __schema__.service_types(id),
    order_id uuid references __schema__.participation_orders(id),
    status varchar(40) not null,
    service_fee numeric(14,2) not null default 0,
    discount_amount numeric(14,2) not null default 0,
    adjustment_amount numeric(14,2) not null default 0,
    total_amount numeric(14,2) not null,
    confirmed_amount numeric(14,2) not null default 0,
    quote_expires_at timestamptz,
    void_reason varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create table if not exists __schema__.service_order_items (
    id uuid primary key,
    service_order_id uuid not null references __schema__.service_orders(id),
    item_name varchar(160) not null,
    quantity numeric(14,2) not null,
    unit_price numeric(14,2) not null,
    amount numeric(14,2) not null,
    remark varchar(500),
    created_at timestamptz not null
);

create table if not exists __schema__.service_payments (
    id uuid primary key,
    service_order_id uuid not null references __schema__.service_orders(id),
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    amount numeric(14,2) not null,
    method varchar(40) not null,
    payer_name varchar(160),
    paid_at timestamptz,
    voucher_file_id varchar(160),
    status varchar(40) not null,
    failure_reason varchar(500),
    confirmed_by uuid,
    confirmed_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);
