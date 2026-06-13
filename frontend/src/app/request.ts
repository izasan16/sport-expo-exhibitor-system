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

  const parseBody = async () => {
    try {
      return (await response.json()) as ApiResponse<T>;
    } catch {
      return undefined;
    }
  };

  if (!response.ok) {
    const body = await parseBody();
    const errorMessage = body?.message || '请求失败，请稍后重试';
    message.error(errorMessage);
    throw new Error(errorMessage);
  }

  const body = await parseBody();
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
