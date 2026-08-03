drop table if exists __schema__.role_permissions;
drop table if exists __schema__.permissions;

alter table if exists __schema__.roles
    drop column if exists updated_by,
    drop column if exists created_by,
    drop column if exists description,
    drop column if exists enabled,
    drop column if exists role_type;
