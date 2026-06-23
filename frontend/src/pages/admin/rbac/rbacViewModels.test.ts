import { describe, expect, it } from 'vitest';
import { buildCheckedPermissionKeys, diffPermissionSelection } from './rbacViewModels';

describe('rbac view models', () => {
  it('builds checked keys for a selected role from permission codes', () => {
    expect(buildCheckedPermissionKeys(['menu.admin.users', 'action.admin.users.create']))
      .toEqual(['action.admin.users.create', 'menu.admin.users']);
  });

  it('computes added and removed permission labels before save', () => {
    expect(diffPermissionSelection(
      ['menu.admin.users'],
      ['menu.admin.users', 'menu.admin.roles'],
      { 'menu.admin.roles': '角色管理' },
    )).toEqual({
      added: ['角色管理'],
      removed: [],
    });
  });
});
