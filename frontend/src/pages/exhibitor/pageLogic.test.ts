import { describe, expect, it } from 'vitest';
import {
  buildBoothSelectionBody,
  buildOrderCreateBody,
  canSubmitBoothSelection,
  compareAreaToOrder,
  filterBoothsForOrderType,
  getOrderNextAction,
  getOrderStepIndex,
  isOrderPayable,
  isOrderSelectable,
  pickLatestActiveOrder,
  sortBoothsForMap,
} from './pageLogic';
import type { ParticipationOrder } from '../../app/domain';
import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';
import { formatArea, formatFriendlyTime, formatMoney, formatShortId } from '../../app/formatters';
import { getOrderStatusMeta } from '../../app/status';

describe('exhibitor page logic', () => {
  it('builds the exact participation order payload with blank zoneId normalized to null', () => {
    expect(
      buildOrderCreateBody({
        eventId: 'event-1',
        zoneId: '',
        areaTierId: 'tier-1',
        boothType: 'STANDARD',
      }),
    ).toEqual({
      eventId: 'event-1',
      exhibitorId: DEMO_EXHIBITOR_ID,
      zoneId: null,
      areaTierId: 'tier-1',
      boothType: 'STANDARD',
    });
  });

  it('enables order actions only for the permitted statuses', () => {
    expect(isOrderPayable('PENDING_PAYMENT')).toBe(true);
    expect(isOrderPayable('PARTIALLY_PAID')).toBe(true);
    expect(isOrderPayable('PAID')).toBe(false);

    expect(isOrderSelectable('PAID')).toBe(true);
    expect(isOrderSelectable('PENDING_PAYMENT')).toBe(false);
  });

  it('compares selected booth area to order area at two decimals', () => {
    expect(compareAreaToOrder(['1.005', '1.005'], '2.01')).toMatchObject({
      selectedDisplay: 2.01,
      orderDisplay: 2.01,
      matches: true,
    });

    expect(compareAreaToOrder(['1.00', '1.00'], '2.10')).toMatchObject({
      selectedDisplay: 2,
      orderDisplay: 2.1,
      matches: false,
      relation: 'short',
    });

    expect(compareAreaToOrder(['1.10', '1.10'], '2.10')).toMatchObject({
      matches: false,
      relation: 'over',
    });
  });

  it('builds the public booth selection submit body without exhibitorId', () => {
    expect(buildBoothSelectionBody('order-1', ['booth-2', 'booth-1'])).toEqual({
      orderId: 'order-1',
      boothIds: ['booth-2', 'booth-1'],
    });
  });

  it('filters available booths to the matched order booth type', () => {
    const booths = [
      { id: 'booth-1', boothType: 'STANDARD' },
      { id: 'booth-2', boothType: 'CUSTOM' },
      { id: 'booth-3', boothType: 'STANDARD' },
    ];

    expect(filterBoothsForOrderType(booths, { boothType: 'STANDARD' })).toEqual([
      { id: 'booth-1', boothType: 'STANDARD' },
      { id: 'booth-3', boothType: 'STANDARD' },
    ]);
  });

  it('sorts booths by booth number for the simplified map', () => {
    expect(sortBoothsForMap([
      { id: '2', boothNo: 'A010' },
      { id: '1', boothNo: 'A002' },
    ])).toEqual([
      { id: '1', boothNo: 'A002' },
      { id: '2', boothNo: 'A010' },
    ]);
  });

  it('prevents booth selection submit while loading or otherwise invalid', () => {
    expect(canSubmitBoothSelection({ loading: true, hasOrder: true, areaMatches: true, selectedCount: 1 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: false, areaMatches: true, selectedCount: 1 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: true, areaMatches: false, selectedCount: 1 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: true, areaMatches: true, selectedCount: 0 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: true, areaMatches: true, selectedCount: 1 })).toBe(true);
  });

  const baseOrder: ParticipationOrder = {
    id: 'e0e5589c-90aa-43b5-beb0-c42878beb4aa',
    eventId: 'event-1',
    exhibitorId: DEMO_EXHIBITOR_ID,
    zoneId: null,
    areaTierId: 'tier-1',
    areaSqm: '18.00',
    boothType: 'STANDARD',
    status: 'PENDING_PAYMENT',
    areaFee: '21600.00',
    serviceFee: '0.00',
    discountAmount: '0.00',
    adjustmentAmount: '0.00',
    totalAmount: '21600.00',
    createdAt: '2026-06-14T10:00:00+08:00',
    updatedAt: '2026-06-14T10:00:00+08:00',
  };

  it('picks latest active order before completed orders', () => {
    const completed = { ...baseOrder, id: 'completed-order', status: 'COMPLETED' as const, createdAt: '2026-06-15T10:00:00+08:00' };
    const pending = { ...baseOrder, id: 'pending-order', status: 'PAID' as const, createdAt: '2026-06-13T10:00:00+08:00' };

    expect(pickLatestActiveOrder([completed, pending])?.id).toBe('pending-order');
  });

  it('derives order steps and next actions', () => {
    expect(getOrderStepIndex('PENDING_SALES_CONFIRM')).toBe(1);
    expect(getOrderStepIndex('PENDING_PAYMENT')).toBe(3);
    expect(getOrderStepIndex('PAID')).toBe(4);
    expect(getOrderNextAction({ order: baseOrder }))
      .toMatchObject({ label: '提交付款', to: `/exhibitor/orders/${baseOrder.id}/payment` });
    expect(getOrderNextAction({ order: { ...baseOrder, status: 'PAID' }, eligibilityStatus: 'AVAILABLE' }))
      .toMatchObject({ label: '选择展位', to: `/exhibitor/orders/${baseOrder.id}/selection` });
  });

  it('formats business values for product UI', () => {
    expect(formatShortId(baseOrder.id)).toBe('e0e5589c');
    expect(formatMoney('21600')).toBe('¥21,600.00');
    expect(formatArea('18')).toBe('18.00 ㎡');
    expect(formatFriendlyTime('2026-06-14T09:30:00+08:00', new Date('2026-06-14T12:00:00+08:00'))).toBe('今天 09:30');
    expect(formatFriendlyTime('2026-06-13T20:15:00+08:00', new Date('2026-06-14T12:00:00+08:00'))).toBe('昨天 20:15');
    expect(formatFriendlyTime('2026-06-10T08:05:00+08:00', new Date('2026-06-14T12:00:00+08:00'))).toBe('2026-06-10 08:05');
    expect(getOrderStatusMeta('PENDING_PAYMENT')).toMatchObject({ label: '待付款' });
  });
});
