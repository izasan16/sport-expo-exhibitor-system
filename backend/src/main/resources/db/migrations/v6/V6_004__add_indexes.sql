create index if not exists idx_sales_assignments_sales_user
    on __schema__.exhibitor_sales_assignments(sales_user_id, assignment_type);

create index if not exists idx_sales_assignments_exhibitor
    on __schema__.exhibitor_sales_assignments(exhibitor_id, assignment_type);

create unique index if not exists uk_exhibitor_primary_sales
    on __schema__.exhibitor_sales_assignments(exhibitor_id)
    where assignment_type = 'PRIMARY';

create index if not exists idx_orders_exhibitor_status
    on __schema__.participation_orders(exhibitor_id, status);

create index if not exists idx_service_applications_exhibitor_status
    on __schema__.service_applications(exhibitor_id, status);

create index if not exists idx_service_orders_exhibitor_status
    on __schema__.service_orders(exhibitor_id, status);

create index if not exists idx_service_payments_order_status
    on __schema__.service_payments(service_order_id, status);

create index if not exists idx_app_values_field_text
    on __schema__.service_application_values(field_code, value_text);
