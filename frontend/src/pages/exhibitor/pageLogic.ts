import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';

export type BoothType = 'STANDARD' | 'CUSTOM';

export type OrderCreateValues = {
  eventId: string;
  zoneId?: string;
  areaTierId: string;
  boothType: BoothType;
};

export function buildOrderCreateBody(values: OrderCreateValues) {
  return {
    eventId: values.eventId,
    exhibitorId: DEMO_EXHIBITOR_ID,
    zoneId: values.zoneId || null,
    areaTierId: values.areaTierId,
    boothType: values.boothType,
  };
}

export function isOrderPayable(status: string) {
  return status === 'PENDING_PAYMENT' || status === 'PARTIALLY_PAID';
}

export function isOrderSelectable(status: string) {
  return status === 'PAID';
}

export type AreaComparison = {
  selectedDisplay: number;
  orderDisplay: number;
  selectedFixed: string;
  orderFixed: string;
  matches: boolean;
  relation: 'short' | 'over' | 'equal';
};

function toNumber(value: string | number): number {
  return typeof value === 'number' ? value : Number(value);
}

export function compareAreaToOrder(selectedAreas: Array<string | number>, orderArea: string | number): AreaComparison {
  const selectedTotal = selectedAreas.reduce<number>((sum, area) => sum + toNumber(area), 0);
  const orderTotal = toNumber(orderArea);
  const selectedFixed = selectedTotal.toFixed(2);
  const orderFixed = orderTotal.toFixed(2);
  const matches = selectedFixed === orderFixed;

  return {
    selectedDisplay: Number(selectedFixed),
    orderDisplay: Number(orderFixed),
    selectedFixed,
    orderFixed,
    matches,
    relation: matches ? 'equal' : Number(selectedFixed) < Number(orderFixed) ? 'short' : 'over',
  };
}

export function buildBoothSelectionBody(orderId: string, boothIds: string[]) {
  return { orderId, boothIds };
}

export function filterBoothsForOrderType<T extends { boothType: string }>(
  booths: T[],
  order: { boothType: string },
): T[] {
  return booths.filter((booth) => booth.boothType === order.boothType);
}

export type BoothSelectionSubmitState = {
  loading: boolean;
  hasOrder: boolean;
  areaMatches: boolean;
  selectedCount: number;
};

export function canSubmitBoothSelection(state: BoothSelectionSubmitState) {
  return !state.loading && state.hasOrder && state.areaMatches && state.selectedCount > 0;
}
