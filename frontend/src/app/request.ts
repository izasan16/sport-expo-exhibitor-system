import { message } from 'antd';

type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
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

export async function apiGet<T>(url: string): Promise<T> {
  const response = await fetch(withApiBaseUrl(url), {
    headers: {
      Accept: 'application/json',
    },
  });

  return parseApiResponse<T>(response);
}

export async function apiPost<T, B = unknown>(url: string, body?: B): Promise<T> {
  const options: RequestInit = {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  };

  if (body !== undefined) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(withApiBaseUrl(url), options);

  return parseApiResponse<T>(response);
}

export async function apiPut<T, B = unknown>(url: string, body?: B): Promise<T> {
  const options: RequestInit = {
    method: 'PUT',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  };

  if (body !== undefined) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(withApiBaseUrl(url), options);

  return parseApiResponse<T>(response);
}

export async function apiDelete<T>(url: string): Promise<T> {
  const response = await fetch(withApiBaseUrl(url), {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
    },
  });

  return parseApiResponse<T>(response);
}

export async function apiMultipartPost<T>(url: string, body: FormData): Promise<T> {
  const response = await fetch(withApiBaseUrl(url), {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
    body,
  });

  return parseApiResponse<T>(response);
}
