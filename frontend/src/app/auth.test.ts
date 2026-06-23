import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { clearAccessToken, getAccessToken, setAccessToken } from './auth';

describe('auth storage helpers', () => {
  beforeEach(() => {
    Object.defineProperty(globalThis, 'localStorage', {
      value: createStorageMock(),
      configurable: true,
    });
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('stores access token after login', () => {
    setAccessToken('abc');

    expect(getAccessToken()).toBe('abc');
  });

  it('clears access token on logout', () => {
    setAccessToken('abc');

    clearAccessToken();

    expect(getAccessToken()).toBeNull();
  });
});

function createStorageMock(): Storage {
  let store = new Map<string, string>();

  return {
    get length() {
      return store.size;
    },
    clear() {
      store = new Map<string, string>();
    },
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null;
    },
    key(index: number) {
      return [...store.keys()][index] ?? null;
    },
    removeItem(key: string) {
      store.delete(key);
    },
    setItem(key: string, value: string) {
      store.set(key, value);
    },
  };
}
