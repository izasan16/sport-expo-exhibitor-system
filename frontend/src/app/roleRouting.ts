import type { CurrentUser, RoleCode } from './types/auth';

const INTERNAL_ROLE_PRIORITY: RoleCode[] = [
  'SYSTEM_ADMIN',
  'ORGANIZER_ADMIN',
  'FINANCE',
  'SERVICE_STAFF',
  'SALES',
];

function availableRoles(user: CurrentUser) {
  return user.activeAvailableRoles.length ? user.activeAvailableRoles : user.roles;
}

export function hasAdminWorkspace(user: CurrentUser) {
  return INTERNAL_ROLE_PRIORITY.some((role) => availableRoles(user).includes(role));
}

export function hasExhibitorWorkspace(user: CurrentUser) {
  return availableRoles(user).includes('EXHIBITOR');
}

export function resolveActiveRole(user: CurrentUser): RoleCode {
  const roles = availableRoles(user);
  if (user.defaultRole && roles.includes(user.defaultRole)) {
    return user.defaultRole;
  }

  return INTERNAL_ROLE_PRIORITY.find((role) => roles.includes(role)) ?? 'EXHIBITOR';
}

export function resolveRestoredActiveRole(user: CurrentUser, storedRole?: string | null): RoleCode {
  const roles = availableRoles(user);
  if (storedRole && roles.includes(storedRole as RoleCode)) {
    return storedRole as RoleCode;
  }

  return resolveActiveRole(user);
}

export function getDefaultPath(user: CurrentUser, activeRole = resolveActiveRole(user)) {
  return activeRole === 'EXHIBITOR' ? '/exhibitor' : '/admin/dashboard';
}

export function getWorkspacePath(role: RoleCode) {
  return role === 'EXHIBITOR' ? '/exhibitor' : '/admin/dashboard';
}
