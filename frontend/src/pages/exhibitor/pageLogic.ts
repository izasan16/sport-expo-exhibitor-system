import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';
import type { EligibilityStatus, OrderStatus, ParticipationOrder, SelectionStatus } from '../../app/domain';

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

export function getOrderStepIndex(status: OrderStatus | string) {
  if (status === 'PENDING_SALES_CONFIRM') return 1;
  if (status === 'PENDING_CONTRACT') return 2;
  if (status === 'PENDING_PAYMENT' || status === 'PARTIALLY_PAID') return 3;
  if (status === 'PAID') return 4;
  if (status === 'COMPLETED') return 5;
  return 0;
}

export function pickLatestActiveOrder(orders: ParticipationOrder[]) {
  const byCreatedDesc = [...orders].sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));
  return byCreatedDesc.find((order) => order.status !== 'COMPLETED') ?? byCreatedDesc[0];
}

export type OrderNextActionInput = {
  order?: ParticipationOrder;
  eligibilityStatus?: EligibilityStatus;
  latestSelectionStatus?: SelectionStatus;
};

export function getOrderNextAction(input: OrderNextActionInput) {
  const { order, eligibilityStatus, latestSelectionStatus } = input;
  if (!order) {
    return { label: '创建参展订单', to: '/exhibitor/orders/new', disabled: false, reason: '' };
  }
  if (order.status === 'PENDING_SALES_CONFIRM') {
    return { label: '等待销售确认', to: `/exhibitor/orders/${order.id}`, disabled: true, reason: '销售确认后才能继续' };
  }
  if (isOrderPayable(order.status)) {
    return { label: '提交付款', to: `/exhibitor/orders/${order.id}/payment`, disabled: false, reason: '' };
  }
  if (order.status === 'PAID' && eligibilityStatus === 'AVAILABLE') {
    return { label: '选择展位', to: `/exhibitor/orders/${order.id}/selection`, disabled: false, reason: '' };
  }
  if (latestSelectionStatus === 'PENDING_REVIEW') {
    return { label: '等待管理员审核', to: `/exhibitor/orders/${order.id}`, disabled: true, reason: '选位申请正在审核' };
  }
  if (latestSelectionStatus === 'CONFIRMED') {
    return { label: '查看选位结果', to: `/exhibitor/orders/${order.id}`, disabled: false, reason: '' };
  }
  return { label: '查看订单', to: `/exhibitor/orders/${order.id}`, disabled: false, reason: '' };
}
