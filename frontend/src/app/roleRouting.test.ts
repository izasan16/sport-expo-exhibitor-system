import { describe, expect, it } from 'vitest';
import { getDefaultPath, resolveRestoredActiveRole } from './roleRouting';
import type { CurrentUser } from './types/auth';

function currentUser(overrides: Partial<CurrentUser> = {}): CurrentUser {
  return {
    id: 'u1',
    username: 'admin',
    displayName: 'Admin',
    roles: ['SYSTEM_ADMIN'],
    defaultRole: 'SYSTEM_ADMIN',
    activeAvailableRoles: ['SYSTEM_ADMIN'],
    exhibitorId: null,
    permissions: [],
    ...overrides,
  };
}

describe('role routing', () => {
  it('maps exhibitor-only users to /exhibitor', () => {
    expect(getDefaultPath(currentUser({
      username: 'exhibitor01',
      displayName: 'Exhibitor',
      roles: ['EXHIBITOR'],
      defaultRole: 'EXHIBITOR',
      activeAvailableRoles: ['EXHIBITOR'],
      exhibitorId: 'e1',
    }))).toBe('/exhibitor');
  });

  it('restores the previous active role only when the user still owns it', () => {
    expect(resolveRestoredActiveRole(currentUser({
      roles: ['SYSTEM_ADMIN', 'EXHIBITOR'],
      activeAvailableRoles: ['SYSTEM_ADMIN', 'EXHIBITOR'],
      exhibitorId: 'e1',
    }), 'EXHIBITOR')).toBe('EXHIBITOR');
  });

  it('falls back to the default role when the stored role is no longer available', () => {
    expect(resolveRestoredActiveRole(currentUser({
      roles: ['SYSTEM_ADMIN', 'EXHIBITOR'],
      activeAvailableRoles: ['SYSTEM_ADMIN', 'EXHIBITOR'],
      exhibitorId: 'e1',
    }), 'FINANCE')).toBe('SYSTEM_ADMIN');
  });
});
