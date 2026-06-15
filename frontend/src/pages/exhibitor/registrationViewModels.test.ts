import { describe, expect, it } from 'vitest';
import {
  formatRegistrationStatus,
  getRegistrationListPrimaryAction,
  getPrimaryRegistrationForHome,
  getRegistrationHomeAction,
} from './registrationViewModels';

describe('exhibitor registration view models', () => {
  it('labels registration statuses in Chinese', () => {
    expect(formatRegistrationStatus('DRAFT')).toBe('草稿');
    expect(formatRegistrationStatus('SUBMITTED')).toBe('待审核');
    expect(formatRegistrationStatus('APPROVED')).toBe('审核通过');
    expect(formatRegistrationStatus('REJECTED')).toBe('已驳回');
    expect(formatRegistrationStatus('CANCELLED')).toBe('已取消');
  });

  it('derives exhibitor home registration actions', () => {
    expect(getRegistrationHomeAction(undefined)).toMatchObject({
      label: '提交报名',
      to: '/exhibitor/registrations/new',
    });
    expect(getRegistrationHomeAction({ id: 'r1', status: 'SUBMITTED' })).toMatchObject({
      label: '查看报名进度',
      disabled: false,
    });
    expect(getRegistrationHomeAction({ id: 'r2', status: 'REJECTED' })).toMatchObject({
      label: '修改并重新提交',
    });
    expect(getRegistrationHomeAction({ id: 'r3', status: 'APPROVED' })).toMatchObject({
      label: '去下单',
    });
  });

  it('prioritizes approved registration for home actions before latest pending records', () => {
    expect(getPrimaryRegistrationForHome([
      { id: 'pending-new', status: 'SUBMITTED', updatedAt: '2026-06-15T10:00:00+08:00' },
      { id: 'approved-old', status: 'APPROVED', updatedAt: '2026-06-14T10:00:00+08:00' },
    ])).toMatchObject({ id: 'approved-old', status: 'APPROVED' });
  });

  it('guides rejected same-event registrations back to editing instead of new registration', () => {
    expect(getRegistrationListPrimaryAction([
      { id: 'rejected-current', status: 'REJECTED', updatedAt: '2026-06-15T10:00:00+08:00' },
    ])).toEqual({
      label: '修改并重新提交',
      to: '/exhibitor/registrations/rejected-current',
    });

    expect(getRegistrationListPrimaryAction([])).toEqual({
      label: '提交报名',
      to: '/exhibitor/registrations/new',
    });
  });
});
