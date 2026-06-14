import { describe, expect, it } from 'vitest';
import { DEMO_EXHIBITOR_ID } from './demoActors';
import {
  buildEventSelectOptions,
  formatAreaTierDisplay,
  formatEventDisplay,
  formatExhibitorDisplay,
  formatPaymentMethod,
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
    expect(formatPaymentMethod('OFFLINE_BANK')).toBe('OFFLINE_BANK');
  });
});
