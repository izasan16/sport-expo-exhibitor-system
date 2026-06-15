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

create table if not exists __schema__.exhibitors (
    id uuid primary key,
    company_name varchar(200) not null,
    credit_code varchar(80),
    contact_name varchar(100),
    contact_phone varchar(40),
    contact_email varchar(120),
    address varchar(300),
    status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create unique index if not exists ux_exhibitors_credit_code
    on __schema__.exhibitors(credit_code)
    where credit_code is not null;

create table if not exists __schema__.event_categories (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    code varchar(80) not null,
    name varchar(120) not null,
    enabled boolean not null default true,
    sort_order int not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique(event_id, code)
);

create table if not exists __schema__.registrations (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    exhibitor_id uuid not null references __schema__.exhibitors(id),
    status varchar(40) not null,
    category varchar(120),
    zone_id uuid references __schema__.zones(id),
    intent_area_sqm numeric(14,2),
    contact_name varchar(100),
    contact_phone varchar(40),
    remark varchar(500),
    review_comment varchar(500),
    reviewed_by uuid,
    reviewed_at timestamptz,
    submitted_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

create unique index if not exists ux_registrations_in_progress_event_exhibitor
    on __schema__.registrations(event_id, exhibitor_id)
    where status in ('DRAFT', 'SUBMITTED');

create index if not exists idx_registrations_event_exhibitor_status
    on __schema__.registrations(event_id, exhibitor_id, status);

create index if not exists idx_registrations_status_submitted
    on __schema__.registrations(status, submitted_at desc);

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

create table if not exists __schema__.participation_orders (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    exhibitor_id uuid not null,
    registration_id uuid references __schema__.registrations(id),
    zone_id uuid references __schema__.zones(id),
    area_tier_id uuid not null references __schema__.area_tiers(id),
    area_sqm numeric(14,2) not null,
    booth_type varchar(60) not null,
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

create table if not exists __schema__.contracts (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    order_id uuid not null references __schema__.participation_orders(id),
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

alter table __schema__.payments add column if not exists failure_reason varchar(500);

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
    exhibitor_remark varchar(500),
    recommended_booths varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

alter table __schema__.booth_selections add column if not exists exhibitor_remark varchar(500);
alter table __schema__.booth_selections add column if not exists recommended_booths varchar(500);

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

create table if not exists __schema__.notifications (
    id uuid primary key,
    event_id uuid references __schema__.events(id),
    recipient_type varchar(40) not null,
    recipient_id uuid not null,
    title varchar(200) not null,
    content varchar(1000) not null,
    type varchar(80) not null,
    related_type varchar(80),
    related_id uuid,
    related_order_id uuid,
    read_at timestamptz,
    created_at timestamptz not null
);

alter table __schema__.notifications add column if not exists related_type varchar(80);
alter table __schema__.notifications add column if not exists related_id uuid;
alter table __schema__.notifications add column if not exists related_order_id uuid;

create index if not exists idx_booths_event_status on __schema__.booths(event_id, status);
create index if not exists idx_participation_orders_status_created_at
    on __schema__.participation_orders(status, created_at desc);
create index if not exists idx_participation_orders_created_at
    on __schema__.participation_orders(created_at);
create index if not exists idx_payments_order_status on __schema__.payments(order_id, status);
create index if not exists idx_payments_status_created_at
    on __schema__.payments(status, created_at desc);
create index if not exists idx_refunds_order_status on __schema__.refunds(order_id, status);
create index if not exists idx_booth_selections_status_created_at
    on __schema__.booth_selections(status, created_at desc);
create index if not exists idx_booth_selections_status_lock_expires_at
    on __schema__.booth_selections(status, lock_expires_at);
create index if not exists idx_booth_locks_event_booth_status on __schema__.booth_locks(event_id, booth_id, status);
create index if not exists idx_booth_locks_selection_id on __schema__.booth_locks(selection_id);
create index if not exists idx_release_tasks_event_status on __schema__.booth_release_tasks(event_id, status);
create index if not exists idx_audit_logs_object on __schema__.audit_logs(object_type, object_id);
create index if not exists idx_notifications_recipient on __schema__.notifications(recipient_id, created_at desc);
create index if not exists idx_notifications_related on __schema__.notifications(related_type, related_id);

create unique index if not exists uidx_area_tiers_event_zone_area
    on __schema__.area_tiers(event_id, coalesce(zone_id, '00000000-0000-0000-0000-000000000000'::uuid), area_sqm);

create unique index if not exists uidx_price_rules_event_zone_booth_type_enabled
    on __schema__.price_rules(event_id, coalesce(zone_id, '00000000-0000-0000-0000-000000000000'::uuid), booth_type)
    where enabled = true;

insert into __schema__.exhibitors(id, company_name, status, created_at, updated_at, created_by, updated_by)
values ('00000000-0000-0000-0000-000000000001', '演示展商', 'ACTIVE', now(), now(),
        '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002')
on conflict (id) do update set company_name = excluded.company_name, status = excluded.status, updated_at = now();

with demo_event as (
    select id
    from __schema__.events
    where year = 2026 and status <> 'ARCHIVED'
    order by created_at asc
    limit 1
)
insert into __schema__.event_categories(id, event_id, code, name, enabled, sort_order, created_at, updated_at)
select gen_random_uuid(), id, 'SPORTS_EQUIPMENT', '体育用品', true, 10, now(), now()
from demo_event
on conflict (event_id, code) do update set name = excluded.name, enabled = true, updated_at = now();

with demo_event as (
    select id
    from __schema__.events
    where year = 2026 and status <> 'ARCHIVED'
    order by created_at asc
    limit 1
)
insert into __schema__.registrations(id, event_id, exhibitor_id, status, category, contact_name, contact_phone,
                                     submitted_at, reviewed_by, reviewed_at, created_at, updated_at, created_by, updated_by)
select gen_random_uuid(), id, '00000000-0000-0000-0000-000000000001', 'APPROVED', 'SPORTS_EQUIPMENT',
       '王老板', '13800000000', now(), '00000000-0000-0000-0000-000000000002', now(), now(), now(),
       '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'
from demo_event
where not exists (
    select 1 from __schema__.registrations r
    where r.event_id = demo_event.id
      and r.exhibitor_id = '00000000-0000-0000-0000-000000000001'
      and r.status = 'APPROVED'
);
