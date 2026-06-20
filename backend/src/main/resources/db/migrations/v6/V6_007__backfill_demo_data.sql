insert into __schema__.exhibitor_sales_assignments (
    id, exhibitor_id, sales_user_id, assignment_type, created_at, updated_at, created_by, updated_by
)
select
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000001',
    u.id,
    'PRIMARY',
    now(),
    now(),
    '00000000-0000-0000-0000-000000000000',
    '00000000-0000-0000-0000-000000000000'
from __schema__.users u
where u.username = 'sales01'
  and exists (
      select 1
      from __schema__.exhibitors e
      where e.id = '00000000-0000-0000-0000-000000000001'
  )
  and not exists (
      select 1
      from __schema__.exhibitor_sales_assignments a
      where a.exhibitor_id = '00000000-0000-0000-0000-000000000001'
        and a.assignment_type = 'PRIMARY'
  )
on conflict (exhibitor_id, sales_user_id, assignment_type) do nothing;
