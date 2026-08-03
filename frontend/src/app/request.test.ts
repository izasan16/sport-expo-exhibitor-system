import { describe, expect, it, vi } from 'vitest';
import { setAccessToken } from './auth';
import { apiDelete, apiMultipartPost } from './request';

describe('request authentication', () => {
  it('sends the current bearer token for delete and multipart requests', async () => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => 'token-1'),
      setItem: vi.fn(),
      removeItem: vi.fn(),
    });
    setAccessToken('token-1');
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true, data: {} }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ success: true, data: {} }), { status: 200 }));
    const form = new FormData();

    await apiDelete('/api/files/1');
    await apiMultipartPost('/api/files/upload', form);

    expect(fetchMock.mock.calls[0][1]?.headers).toMatchObject({ Authorization: 'Bearer token-1' });
    expect(fetchMock.mock.calls[1][1]?.headers).toMatchObject({ Authorization: 'Bearer token-1' });
    fetchMock.mockRestore();
    vi.unstubAllGlobals();
  });
});
