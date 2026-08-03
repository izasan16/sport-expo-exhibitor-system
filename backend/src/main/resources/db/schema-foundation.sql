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

alter table __schema__.events
    add column if not exists booth_source_mode varchar(40) not null default 'FORM_ONLY';

alter table __schema__.events
    add column if not exists visual_scene_enabled boolean not null default false;

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

create table if not exists __schema__.venue_scenes (
    id uuid primary key,
    event_id uuid not null unique references __schema__.events(id),
    status varchar(40) not null,
    canvas_width int not null,
    canvas_height int not null,
    pixel_to_meter_ratio numeric(14,4) not null default 0,
    published_revision_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid
);

alter table __schema__.venue_scenes
    add column if not exists published_background_visible_to_sales boolean not null default true;

alter table __schema__.venue_scenes
    add column if not exists published_background_visible_to_exhibitor boolean not null default false;

create table if not exists __schema__.scene_revisions (
    id uuid primary key,
    scene_id uuid not null references __schema__.venue_scenes(id),
    revision_no int not null,
    status varchar(40) not null,
    change_summary varchar(500),
    snapshot_json jsonb not null,
    published_at timestamptz,
    published_by uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(scene_id, revision_no)
);

create table if not exists __schema__.scene_features (
    id uuid primary key,
    scene_revision_id uuid not null references __schema__.scene_revisions(id),
    event_id uuid not null references __schema__.events(id),
    feature_type varchar(40) not null,
    geometry_type varchar(40) not null,
    geometry_json jsonb not null,
    label varchar(200),
    code varchar(80),
    layer_order int not null default 0,
    style_json jsonb not null default '{}'::jsonb,
    meta_json jsonb not null default '{}'::jsonb,
    area_px2 numeric(14,2),
    area_sqm numeric(14,2),
    locked boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists __schema__.stored_files (
    id uuid primary key,
    original_name varchar(255) not null,
    mime_type varchar(120) not null,
    storage_path varchar(500) not null,
    public_url varchar(500) not null,
    file_size bigint not null,
    width int,
    height int,
    created_at timestamptz not null,
    created_by uuid
);

alter table __schema__.stored_files
    add column if not exists scene_conversion_job_id uuid;

create table if not exists __schema__.scene_background_assets (
    id uuid primary key,
    scene_id uuid not null references __schema__.venue_scenes(id),
    scene_revision_id uuid not null references __schema__.scene_revisions(id),
    file_id uuid not null references __schema__.stored_files(id),
    file_name varchar(255) not null,
    mime_type varchar(120) not null,
    source_type varchar(40) not null,
    width int,
    height int,
    offset_x numeric(14,2) not null default 0,
    offset_y numeric(14,2) not null default 0,
    scale numeric(14,4) not null default 1,
    opacity numeric(5,4) not null default 0.45,
    locked boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by uuid,
    updated_by uuid,
    unique(scene_revision_id)
);

alter table __schema__.scene_background_assets
    add column if not exists render_format varchar(40) not null default 'SVG';

alter table __schema__.scene_background_assets
    add column if not exists rotation_deg numeric(10,4) not null default 0;

alter table __schema__.scene_background_assets
    add column if not exists visible boolean not null default true;

alter table __schema__.scene_background_assets
    add column if not exists z_index int not null default 0;

create table if not exists __schema__.scene_source_files (
    id uuid primary key,
    scene_id uuid not null references __schema__.venue_scenes(id),
    scene_revision_id uuid not null references __schema__.scene_revisions(id),
    file_id uuid not null references __schema__.stored_files(id),
    source_format varchar(20) not null,
    original_name varchar(255) not null,
    file_size bigint not null,
    upload_status varchar(40) not null,
    created_at timestamptz not null,
    created_by uuid
);

create table if not exists __schema__.scene_conversion_jobs (
    id uuid primary key,
    scene_id uuid not null references __schema__.venue_scenes(id),
    scene_revision_id uuid not null references __schema__.scene_revisions(id),
    source_file_id uuid not null references __schema__.scene_source_files(id),
    job_status varchar(40) not null,
    target_format varchar(40) not null,
    result_file_id uuid references __schema__.stored_files(id),
    error_code varchar(80),
    error_message varchar(500),
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz not null
);

alter table __schema__.scene_conversion_jobs
    drop constraint if exists fk_scene_conversion_jobs_source_file_match;

alter table __schema__.scene_conversion_jobs
    drop constraint if exists fk_scene_conversion_jobs_scene_revision_match;

alter table __schema__.scene_source_files
    drop constraint if exists fk_scene_source_files_scene_revision_match;

alter table __schema__.scene_source_files
    drop constraint if exists ux_scene_source_files_id_scene_revision_id_scene_id;

alter table __schema__.scene_revisions
    drop constraint if exists ux_scene_revisions_id_scene_id;

alter table __schema__.scene_revisions
    add constraint ux_scene_revisions_id_scene_id
        unique (id, scene_id);

alter table __schema__.scene_source_files
    add constraint ux_scene_source_files_id_scene_revision_id_scene_id
        unique (id, scene_revision_id, scene_id);

alter table __schema__.scene_source_files
    add constraint fk_scene_source_files_scene_revision_match
        foreign key (scene_revision_id, scene_id)
        references __schema__.scene_revisions(id, scene_id)
        not valid;

alter table __schema__.scene_conversion_jobs
    add constraint fk_scene_conversion_jobs_scene_revision_match
        foreign key (scene_revision_id, scene_id)
        references __schema__.scene_revisions(id, scene_id)
        not valid;

alter table __schema__.scene_conversion_jobs
    add constraint fk_scene_conversion_jobs_source_file_match
        foreign key (source_file_id, scene_revision_id, scene_id)
        references __schema__.scene_source_files(id, scene_revision_id, scene_id)
        not valid;

alter table __schema__.stored_files
    drop constraint if exists fk_stored_files_scene_conversion_job;
alter table __schema__.stored_files
    add constraint fk_stored_files_scene_conversion_job
        foreign key (scene_conversion_job_id)
        references __schema__.scene_conversion_jobs(id)
        not valid;

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
    registration_id uuid,
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

create table if not exists __schema__.booth_scene_bindings (
    id uuid primary key,
    event_id uuid not null references __schema__.events(id),
    scene_revision_id uuid not null references __schema__.scene_revisions(id),
    scene_feature_id uuid not null references __schema__.scene_features(id),
    booth_id uuid not null references __schema__.booths(id),
    binding_status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique(scene_feature_id),
    unique(scene_revision_id, booth_id)
);

alter table __schema__.booth_scene_bindings
    drop constraint if exists booth_scene_bindings_booth_id_key;

create table if not exists __schema__.scene_edit_locks (
    event_id uuid primary key references __schema__.events(id),
    revision_id uuid not null references __schema__.scene_revisions(id),
    locked_by uuid not null,
    locked_by_name varchar(160) not null,
    heartbeat_at timestamptz not null,
    expires_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
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
alter table __schema__.participation_orders add column if not exists registration_id uuid;
alter table __schema__.participation_orders drop constraint if exists fk_participation_orders_registration_id;
alter table __schema__.participation_orders
    add constraint fk_participation_orders_registration_id
    foreign key (registration_id)
    references __schema__.registrations(id)
    not valid;

create index if not exists idx_booths_event_status on __schema__.booths(event_id, status);
create index if not exists idx_participation_orders_status_created_at
    on __schema__.participation_orders(status, created_at desc);
create index if not exists idx_participation_orders_created_at
    on __schema__.participation_orders(created_at);
create index if not exists idx_scene_revisions_scene_status
    on __schema__.scene_revisions(scene_id, status, revision_no desc);
create index if not exists idx_scene_features_revision_type
    on __schema__.scene_features(scene_revision_id, feature_type, layer_order);
create index if not exists idx_scene_background_assets_revision
    on __schema__.scene_background_assets(scene_revision_id);
create index if not exists idx_scene_source_files_scene_revision
    on __schema__.scene_source_files(scene_revision_id);
create index if not exists idx_scene_conversion_jobs_scene_revision
    on __schema__.scene_conversion_jobs(scene_revision_id);
create index if not exists idx_stored_files_created_at
    on __schema__.stored_files(created_at desc);
create index if not exists idx_booth_scene_bindings_event_revision
    on __schema__.booth_scene_bindings(event_id, scene_revision_id);
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
