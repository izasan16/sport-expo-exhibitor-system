import { describe, expect, it } from 'vitest';
import type { AdminDashboardResponse, ParticipationOrder } from '../../../app/domain';
import { buildDashboardViewModel, countTodayOrders, formatDashboardGeneratedAt } from './dashboardLogic';

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

  it('formats generatedAt and falls back when the server omits it', () => {
    const now = new Date('2026-06-14T12:00:00+08:00');

    expect(formatDashboardGeneratedAt('2026-06-14T09:30:00+08:00', now)).toBe('今天 09:30');
    expect(formatDashboardGeneratedAt(null, now)).toBe('时间未记录');
    expect(formatDashboardGeneratedAt(undefined, now)).toBe('时间未记录');
  });

  it('keeps dashboard response counts and normalizes missing top lists', () => {
    const response: AdminDashboardResponse = {
      generatedAt: null,
      pendingSalesConfirmCount: 8,
      pendingFinanceConfirmCount: 3,
      pendingSelectionReviewCount: 2,
      todayNewOrderCount: 5,
      pendingSalesConfirmOrders: [baseOrder],
      pendingFinanceConfirmPayments: undefined,
      pendingSelectionReviews: undefined,
    };

    expect(buildDashboardViewModel(response, new Date('2026-06-14T12:00:00+08:00'))).toMatchObject({
      generatedAtDisplay: '时间未记录',
      counts: {
        pendingSalesConfirm: 8,
        pendingFinanceConfirm: 3,
        pendingSelectionReview: 2,
        todayNewOrder: 5,
      },
      pendingSalesConfirmOrders: [baseOrder],
      pendingFinanceConfirmPayments: [],
      pendingSelectionReviews: [],
    });
  });
});
