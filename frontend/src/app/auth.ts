import type { CurrentUser, CurrentUserApiResponse, LoginApiResponse, RoleCode } from './types/auth';

const TOKEN_KEY = 'access_token';
const ACTIVE_ROLE_KEY = 'active_role';
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
  errorCode?: string | null;
};

function withApiBaseUrl(url: string): string {
  if (!apiBaseUrl || /^https?:\/\//i.test(url)) {
    return url;
  }

  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`;
}

function parseApiError(body: ApiResponse<unknown> | undefined, fallback: string) {
  return body?.message || fallback;
}

async function parseBody<T>(response: Response): Promise<ApiResponse<T> | undefined> {
  try {
    return (await response.json()) as ApiResponse<T>;
  } catch {
    return undefined;
  }
}

export function getAccessToken() {
  return globalThis.localStorage?.getItem(TOKEN_KEY) ?? null;
}

export function setAccessToken(token: string) {
  globalThis.localStorage?.setItem(TOKEN_KEY, token);
}

export function clearAccessToken() {
  globalThis.localStorage?.removeItem(TOKEN_KEY);
}

export function getStoredActiveRole() {
  return globalThis.localStorage?.getItem(ACTIVE_ROLE_KEY) ?? null;
}

export function setStoredActiveRole(role: RoleCode) {
  globalThis.localStorage?.setItem(ACTIVE_ROLE_KEY, role);
}

export function clearStoredActiveRole() {
  globalThis.localStorage?.removeItem(ACTIVE_ROLE_KEY);
}

export function normalizeCurrentUser(payload: CurrentUserApiResponse): CurrentUser {
  return {
    id: payload.userId,
    username: payload.username,
    displayName: payload.displayName,
    roles: payload.roles,
    defaultRole: payload.defaultRole ?? null,
    activeAvailableRoles: payload.activeAvailableRoles?.length ? payload.activeAvailableRoles : payload.roles,
    exhibitorId: payload.exhibitorId ?? null,
    permissions: [...new Set(payload.permissions ?? [])].sort(),
  };
}

export async function loginWithPassword(username: string, password: string) {
  const response = await fetch(withApiBaseUrl('/api/auth/login'), {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username,
      password,
    }),
  });

  const body = await parseBody<LoginApiResponse>(response);
  if (!response.ok || !body?.success || !body.data?.accessToken) {
    throw new Error(parseApiError(body, '登录失败，请检查账号或密码'));
  }

  return body.data.accessToken;
}

export async function fetchCurrentUser(token = getAccessToken()) {
  const response = await fetch(withApiBaseUrl('/api/auth/me'), {
    headers: {
      Accept: 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  const body = await parseBody<CurrentUserApiResponse>(response);
  if (!response.ok || !body?.success || !body.data) {
    throw new Error(parseApiError(body, '获取当前用户失败'));
  }

  return normalizeCurrentUser(body.data);
}

export async function logoutRequest() {
  const token = getAccessToken();
  if (!token) {
    return;
  }

  await fetch(withApiBaseUrl('/api/auth/logout'), {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${token}`,
    },
  });
}
