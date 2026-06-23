import { DEMO_EXHIBITOR_ID } from './demoActors';
import type { EventCategory, ExhibitorStatus, RegistrationStatus } from './domain';
import { formatArea, formatShortId } from './formatters';
import { ROLE_LABELS } from './types/rbac';

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

const boothTypeLabels: Record<string, string> = {
  STANDARD: '标准展位',
  CUSTOM: '特装展位',
};

const registrationStatusLabels: Record<RegistrationStatus, string> = {
  DRAFT: '草稿',
  SUBMITTED: '待审核',
  APPROVED: '审核通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
};

const exhibitorStatusLabels: Record<ExhibitorStatus, string> = {
  ACTIVE: '正常',
  SUSPENDED: '已停用',
  ARCHIVED: '已归档',
};

export function formatPaymentMethod(method: string | null | undefined) {
  if (!method) return '-';
  return paymentMethodLabels[method] ?? method;
}

export function formatSupportedPaymentMethod(method: string | null | undefined) {
  if (!method) return '未填写';
  return paymentMethodLabels[method] ?? '未知付款方式';
}

export function formatBoothType(boothType: string | null | undefined) {
  if (!boothType) return '未填写';
  return boothTypeLabels[boothType] ?? boothType;
}

export function formatRegistrationStatus(status: RegistrationStatus | string | null | undefined) {
  if (!status) return '-';
  return registrationStatusLabels[status as RegistrationStatus] ?? status;
}

export function formatExhibitorStatus(status: ExhibitorStatus | string | null | undefined) {
  if (!status) return '-';
  return exhibitorStatusLabels[status as ExhibitorStatus] ?? status;
}

export function formatRoleDisplay(roleCode: string | null | undefined) {
  if (!roleCode) return '-';
  return ROLE_LABELS[roleCode as keyof typeof ROLE_LABELS] ?? roleCode;
}

export function formatEventCategoryDisplay(category: Pick<EventCategory, 'code' | 'name'> | null | undefined) {
  if (!category) return '-';
  return `${category.name}（${category.code}）`;
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
