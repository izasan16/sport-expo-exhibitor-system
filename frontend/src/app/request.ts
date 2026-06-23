import { message } from 'antd';
import { clearAccessToken, clearStoredActiveRole, getAccessToken } from './auth';

type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
  errorCode?: string | null;
};

type ApiRequestOptions = RequestInit & {
  authMode?: 'required' | 'public';
  redirectOnForbidden?: boolean;
};

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

function withApiBaseUrl(url: string): string {
  if (!apiBaseUrl || /^https?:\/\//i.test(url)) {
    return url;
  }

  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`;
}

async function parseApiResponse<T>(response: Response): Promise<T> {
  const body = await parseBody<T>(response);

  if (!response.ok) {
    const errorMessage = body?.message || '请求失败，请稍后重试';
    message.error(errorMessage);
    throw new Error(errorMessage);
  }

  if (!body) {
    message.error('请求失败，请稍后重试');
    throw new Error('Request failed');
  }

  if (!body.success) {
    message.error(body.message || '请求失败，请稍后重试');
    throw new Error(body.message || 'Request failed');
  }

  return body.data;
}

async function parseBody<T>(response: Response): Promise<ApiResponse<T> | undefined> {
  try {
    return (await response.json()) as ApiResponse<T>;
  } catch {
    return undefined;
  }
}

async function apiRequest<T>(url: string, options: ApiRequestOptions = {}): Promise<T> {
  const token = getAccessToken();
  const response = await fetch(withApiBaseUrl(url), {
    ...options,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (response.status === 401 && options.authMode !== 'public') {
    clearAccessToken();
    clearStoredActiveRole();
    const returnTo = `${window.location.pathname}${window.location.search}`;
    window.location.assign(`/401?returnTo=${encodeURIComponent(returnTo)}`);
    throw new Error('未登录或登录已过期');
  }

  if (response.status === 403 && options.redirectOnForbidden !== false) {
    const body = await parseBody<unknown>(response);
    const requiredPermission = typeof body?.errorCode === 'string' ? body.errorCode : '';
    window.location.assign(`/403?requiredPermission=${encodeURIComponent(requiredPermission)}`);
    throw new Error(body?.message || '无权限访问');
  }

  return parseApiResponse<T>(response);
}

export async function apiGet<T>(url: string, options?: ApiRequestOptions): Promise<T> {
  return apiRequest<T>(url, {
    ...options,
    method: 'GET',
    headers: {
      Accept: 'application/json',
      ...options?.headers,
    },
  });
}

export async function apiPost<T, B = unknown>(url: string, body?: B, options?: ApiRequestOptions): Promise<T> {
  const requestOptions: RequestInit = {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  };

  if (body !== undefined) {
    requestOptions.body = JSON.stringify(body);
  }

  return apiRequest<T>(url, {
    ...requestOptions,
    ...options,
  });
}

export async function apiPut<T, B = unknown>(url: string, body?: B, options?: ApiRequestOptions): Promise<T> {
  const requestOptions: RequestInit = {
    method: 'PUT',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  };

  if (body !== undefined) {
    requestOptions.body = JSON.stringify(body);
  }

  return apiRequest<T>(url, {
    ...requestOptions,
    ...options,
  });
}
