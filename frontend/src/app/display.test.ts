import { describe, expect, it } from 'vitest';
import { DEMO_EXHIBITOR_ID } from './demoActors';
import {
  buildEventSelectOptions,
  formatAreaTierDisplay,
  formatBoothType,
  formatEventCategoryDisplay,
  formatEventDisplay,
  formatExhibitorDisplay,
  formatExhibitorStatus,
  formatPaymentMethod,
  formatRoleDisplay,
  formatRegistrationStatus,
  formatSupportedPaymentMethod,
} from './display';

describe('business display helpers', () => {
  const events = [
    { id: 'event-1', name: '上海国际体育用品展', year: 2026, status: 'PLANNING' },
  ];

  it('shows business names before falling back to short ids', () => {
    expect(formatEventDisplay('event-1', events)).toBe('上海国际体育用品展（2026）');
    expect(formatExhibitorDisplay(DEMO_EXHIBITOR_ID)).toBe('演示展商');
    expect(formatEventDisplay('12345678-1234-1234-1234-123456789abc', events)).toBe('展会 12345678');
  });

  it('builds event select options for forms and table search', () => {
    expect(buildEventSelectOptions(events)).toEqual([
      { label: '上海国际体育用品展（2026）', value: 'event-1' },
    ]);
  });

  it('shows area tier as readable area and optional zone summary', () => {
    expect(formatAreaTierDisplay('tier-1', [{ id: 'tier-1', eventId: 'event-1', areaSqm: '18.00', enabled: true }]))
      .toBe('18.00 ㎡');
    expect(formatAreaTierDisplay('tier-2', [{ id: 'tier-2', eventId: 'event-1', zoneId: 'zone-a', areaSqm: '36.00', enabled: true }]))
      .toBe('36.00 ㎡ / 分区 zone-a');
  });

  it('formats supported payment methods and leaves unsupported values raw', () => {
    expect(formatPaymentMethod('OFFLINE_TRANSFER')).toBe('银行转账');
    expect(formatPaymentMethod('OFFLINE_CASH')).toBe('现金');
    expect(formatPaymentMethod('OFFLINE_CHECK')).toBe('支票');
    expect(formatPaymentMethod('OFFLINE_BANK')).toBe('OFFLINE_BANK');
  });

  it('formats booth type values as business copy', () => {
    expect(formatBoothType('STANDARD')).toBe('标准展位');
    expect(formatBoothType('CUSTOM')).toBe('特装展位');
    expect(formatBoothType('UNKNOWN')).toBe('UNKNOWN');
    expect(formatBoothType(null)).toBe('未填写');
  });

  it('formats existing payment records with safe supported-method fallback', () => {
    expect(formatSupportedPaymentMethod('OFFLINE_TRANSFER')).toBe('银行转账');
    expect(formatSupportedPaymentMethod('OFFLINE_CASH')).toBe('现金');
    expect(formatSupportedPaymentMethod('OFFLINE_CHECK')).toBe('支票');
    expect(formatSupportedPaymentMethod('OFFLINE_BANK')).toBe('未知付款方式');
    expect(formatSupportedPaymentMethod(null)).toBe('未填写');
  });

  it('formats exhibitor registration business values', () => {
    expect(formatRegistrationStatus('SUBMITTED')).toBe('待审核');
    expect(formatRegistrationStatus('APPROVED')).toBe('审核通过');
    expect(formatExhibitorStatus('ACTIVE')).toBe('正常');
    expect(formatExhibitorStatus('SUSPENDED')).toBe('已停用');
    expect(formatEventCategoryDisplay({ code: 'SPORTS', name: '体育用品' })).toBe('体育用品（SPORTS）');
    expect(formatEventCategoryDisplay(undefined)).toBe('-');
  });

  it('formats role codes into readable workspace labels', () => {
    expect(formatRoleDisplay('SYSTEM_ADMIN')).toBe('系统管理员');
    expect(formatRoleDisplay('EXHIBITOR')).toBe('展商');
    expect(formatRoleDisplay('UNKNOWN_ROLE')).toBe('UNKNOWN_ROLE');
    expect(formatRoleDisplay(null)).toBe('-');
  });
});
