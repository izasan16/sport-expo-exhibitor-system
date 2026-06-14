import { describe, expect, it } from 'vitest';
import type { ParticipationOrder } from '../../../app/domain';
import { countTodayOrders } from './dashboardLogic';

const baseOrder: ParticipationOrder = {
  id: 'order-1',
  eventId: 'event-1',
  exhibitorId: 'exhibitor-1',
  zoneId: null,
  areaTierId: 'tier-1',
  areaSqm: '18.00',
  boothType: 'STANDARD',
  status: 'PENDING_SALES_CONFIRM',
  areaFee: '21600.00',
  serviceFee: '0.00',
  discountAmount: '0.00',
  adjustmentAmount: '0.00',
  totalAmount: '21600.00',
  createdAt: '2026-06-14T09:00:00+08:00',
  updatedAt: '2026-06-14T09:00:00+08:00',
};

describe('dashboard logic', () => {
  it('counts orders created on the current local date', () => {
    expect(countTodayOrders([
      baseOrder,
      { ...baseOrder, id: 'order-2', createdAt: '2026-06-13T23:59:00+08:00' },
    ], new Date('2026-06-14T12:00:00+08:00'))).toBe(1);
  });
});
