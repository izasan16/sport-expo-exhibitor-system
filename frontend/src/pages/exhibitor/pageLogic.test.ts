import { describe, expect, it } from 'vitest';
import {
  buildBoothSelectionBody,
  buildOrderCreateBody,
  canSubmitBoothSelection,
  compareAreaToOrder,
  filterBoothsForOrderType,
  isOrderPayable,
  isOrderSelectable,
} from './pageLogic';
import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';

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

  it('prevents booth selection submit while loading or otherwise invalid', () => {
    expect(canSubmitBoothSelection({ loading: true, hasOrder: true, areaMatches: true, selectedCount: 1 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: false, areaMatches: true, selectedCount: 1 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: true, areaMatches: false, selectedCount: 1 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: true, areaMatches: true, selectedCount: 0 })).toBe(false);
    expect(canSubmitBoothSelection({ loading: false, hasOrder: true, areaMatches: true, selectedCount: 1 })).toBe(true);
  });
});
