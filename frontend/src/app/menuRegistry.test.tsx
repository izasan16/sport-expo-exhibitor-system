import { describe, expect, it } from 'vitest';
import { buildAdminMenu } from './menuRegistry';

describe('buildAdminMenu', () => {
  it('shows permission center menu only when the current user has matching menu permissions', () => {
    const menu = buildAdminMenu([
      'menu.admin.users',
      'menu.admin.roles',
      'menu.admin.permissions',
    ]);

    expect(menu.map((item) => item.key)).toContain('/admin/permissions');
    expect(menu.map((item) => item.key)).not.toContain('/admin/role-permissions');
  });
});
