import type { ReactNode } from 'react';
import type { RoleCode } from './auth';

export const ROLE_LABELS: Record<RoleCode, string> = {
  SYSTEM_ADMIN: '系统管理员',
  ORGANIZER_ADMIN: '主办方管理员',
  SALES: '销售',
  FINANCE: '财务',
  SERVICE_STAFF: '服务人员',
  EXHIBITOR: '展商',
};

export const USER_TYPE_LABELS: Record<string, string> = {
  INTERNAL: '内部用户',
  ADMIN: '后台管理员',
  STAFF: '内部人员',
  EXHIBITOR: '展商账号',
};

export const USER_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '启用',
  DISABLED: '禁用',
};

export const ROLE_TYPE_LABELS: Record<string, string> = {
  SYSTEM: '系统预置',
  CUSTOM: '自定义',
};

export const PERMISSION_RESOURCE_TYPE_LABELS: Record<string, string> = {
  MENU: '菜单',
  PAGE: '页面',
  ACTION: '操作',
};

export type AdminMenuItem = {
  key: string;
  path: string;
  label: string;
  permission: string;
  icon: ReactNode;
};

export type UserSummary = {
  id: string;
  username: string;
  displayName: string;
  phone?: string | null;
  email?: string | null;
  userType: string;
  exhibitorId?: string | null;
  status: string;
  defaultRole?: string | null;
  roleCodes: string[];
};

export type UserDetail = UserSummary & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RoleSummary = {
  id: string;
  code: string;
  name: string;
  roleType: string;
  enabled: boolean;
  description?: string | null;
};

export type RoleDetail = RoleSummary & {
  permissionCodes: string[];
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type PermissionResourceType = 'MENU' | 'PAGE' | 'ACTION';

export type PermissionResource = {
  id: string;
  code: string;
  name: string;
  resourceType: PermissionResourceType;
  moduleKey: string;
  parentId?: string | null;
  routePath?: string | null;
  actionKey?: string | null;
  sortOrder: number;
  enabled: boolean;
  description?: string | null;
};

export type PermissionTreeNode = PermissionResource & {
  children: PermissionTreeNode[];
};

export type CreateUserPayload = {
  username: string;
  password: string;
  displayName: string;
  phone?: string | null;
  email?: string | null;
  userType: string;
  exhibitorId?: string | null;
  status: string;
  roleCodes: string[];
  defaultRole: string;
};

export type UpdateUserPayload = {
  username: string;
  displayName: string;
  phone?: string | null;
  email?: string | null;
  userType: string;
  exhibitorId?: string | null;
};

export type UpdateUserRolesPayload = {
  roleCodes: string[];
  defaultRole: string;
};

export type CreateRolePayload = {
  code: string;
  name: string;
  description?: string | null;
};

export type UpdateRolePayload = CreateRolePayload;

export type PermissionRequestPayload = {
  code: string;
  name: string;
  resourceType: PermissionResourceType;
  moduleKey: string;
  parentId?: string | null;
  routePath?: string | null;
  actionKey?: string | null;
  sortOrder: number;
  enabled: boolean;
  description?: string | null;
};
