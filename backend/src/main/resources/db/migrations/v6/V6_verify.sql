select count(*) as role_count
from __schema__.roles
where code in (
    'SYSTEM_ADMIN',
    'ORGANIZER_ADMIN',
    'SALES',
    'FINANCE',
    'SERVICE_STAFF',
    'EXHIBITOR'
);

select username
from __schema__.users
where username in (
    'admin',
    'organizer',
    'sales01',
    'finance01',
    'service01',
    'exhibitor01'
)
order by username;

select count(*) as user_role_count
from __schema__.user_roles;

select count(*) as service_type_count
from __schema__.service_types;

select count(*) as primary_sales_assignment_count
from __schema__.exhibitor_sales_assignments
where assignment_type = 'PRIMARY';

select table_name
from information_schema.tables
where table_schema = '__schema__'
  and table_name in (
      'users',
      'roles',
      'user_roles',
      'exhibitor_sales_assignments',
      'sales_follow_ups',
      'service_types',
      'service_fields',
      'service_applications',
      'service_application_booths',
      'service_application_values',
      'service_application_attachments',
      'service_orders',
      'service_order_items',
      'service_payments'
  )
order by table_name;
