import { message } from 'antd';

type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
};

export async function apiGet<T>(url: string): Promise<T> {
  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    message.error('请求失败，请稍后重试');
    throw new Error(`Request failed: ${response.status}`);
  }

  const body = (await response.json()) as ApiResponse<T>;
  if (!body.success) {
    message.error(body.message || '请求失败，请稍后重试');
    throw new Error(body.message || 'Request failed');
  }

  return body.data;
}
