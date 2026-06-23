export type RoleCode =
  | 'SYSTEM_ADMIN'
  | 'ORGANIZER_ADMIN'
  | 'SALES'
  | 'FINANCE'
  | 'SERVICE_STAFF'
  | 'EXHIBITOR';

export type CurrentUser = {
  id: string;
  username: string;
  displayName: string;
  roles: RoleCode[];
  defaultRole?: RoleCode | null;
  activeAvailableRoles: RoleCode[];
  exhibitorId?: string | null;
  permissions: string[];
};

export type CurrentUserApiResponse = {
  userId: string;
  username: string;
  displayName: string;
  roles: RoleCode[];
  defaultRole?: RoleCode | null;
  activeAvailableRoles: RoleCode[];
  exhibitorId?: string | null;
  permissions: string[];
};

export type LoginApiResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  displayName: string;
  exhibitorId?: string | null;
  roles: RoleCode[];
};
