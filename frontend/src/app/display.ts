import { DEMO_EXHIBITOR_ID } from './demoActors';
import { formatArea, formatShortId } from './formatters';

export type EventLookupSource = {
  id: string;
  name: string;
  year: number;
  status?: string;
};

export type AreaTierLookupSource = {
  id: string;
  eventId: string;
  zoneId?: string | null;
  areaSqm: number | string;
  enabled: boolean;
};

export function formatEventDisplay(eventId: string | null | undefined, events: EventLookupSource[]) {
  if (!eventId) return '-';
  const event = events.find((item) => item.id === eventId);
  return event ? `${event.name}（${event.year}）` : `展会 ${formatShortId(eventId)}`;
}

export function formatExhibitorDisplay(exhibitorId: string | null | undefined) {
  if (!exhibitorId) return '-';
  if (exhibitorId === DEMO_EXHIBITOR_ID) return '演示展商';
  return `展商 ${formatShortId(exhibitorId)}`;
}

export function formatAreaTierDisplay(areaTierId: string | null | undefined, areaTiers: AreaTierLookupSource[]) {
  if (!areaTierId) return '-';
  const tier = areaTiers.find((item) => item.id === areaTierId);
  if (!tier) return `档位 ${formatShortId(areaTierId)}`;
  return tier.zoneId ? `${formatArea(tier.areaSqm)} / 分区 ${formatShortId(tier.zoneId)}` : formatArea(tier.areaSqm);
}

const paymentMethodLabels: Record<string, string> = {
  OFFLINE_TRANSFER: '银行转账',
  OFFLINE_CASH: '现金',
  OFFLINE_CHECK: '支票',
};

export function formatPaymentMethod(method: string | null | undefined) {
  if (!method) return '-';
  return paymentMethodLabels[method] ?? method;
}

export function buildEventSelectOptions(events: EventLookupSource[]) {
  return events.map((event) => ({
    label: formatEventDisplay(event.id, events),
    value: event.id,
  }));
}

export function buildValueEnum(options: Array<{ label: string; value: string }>) {
  return Object.fromEntries(options.map((option) => [option.value, { text: option.label }]));
}
