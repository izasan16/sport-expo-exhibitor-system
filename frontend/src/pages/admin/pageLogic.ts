import type { BatchConfirmOrdersResponse, Booth, BoothSelection, ParticipationOrder, PaymentRecord } from '../../app/domain';
import { formatShortId } from '../../app/formatters';

export const ADMIN_REASON_MAX_LENGTH = 500;

export function buildBatchConfirmFailureItems(response?: BatchConfirmOrdersResponse | null) {
  if (!response || response.failureCount <= 0) return [];
  return response.failures.map((failure) => ({
    key: failure.orderId,
    text: `${formatShortId(failure.orderId)}：${failure.reason || '未知原因'}`,
  }));
}

export function validateRequiredAdminReason(value?: string | null) {
  if (!value?.trim()) return '请输入原因';
  if (value.length > ADMIN_REASON_MAX_LENGTH) return '原因不能超过 500 字';
  return undefined;
}

export function buildAdminReasonRules(emptyMessage: string) {
  return [
    {
      validator: async (_: unknown, value?: string | null) => {
        const message = validateRequiredAdminReason(value);
        if (!message) return;
        throw new Error(message === '请输入原因' ? emptyMessage : message);
      },
    },
  ];
}

export function isOrderSalesConfirmable(order: { status: string }) {
  return order.status === 'PENDING_SALES_CONFIRM';
}

export function filterConfirmableSelectedKeys<T extends { id: string; status: string }>(
  selectedKeys: Array<string | number>,
  currentRows: T[],
) {
  const selectedKeySet = new Set(selectedKeys.map(String));
  return currentRows
    .filter((row) => selectedKeySet.has(row.id) && isOrderSalesConfirmable(row))
    .map((row) => row.id);
}

export function joinPaymentsWithOrders(payments: PaymentRecord[], orders: ParticipationOrder[]) {
  const ordersById = new Map(orders.map((order) => [order.id, order]));
  return payments.map((payment) => ({
    ...payment,
    eventId: payment.eventId ?? ordersById.get(payment.orderId)?.eventId ?? null,
    exhibitorId: ordersById.get(payment.orderId)?.exhibitorId ?? null,
  }));
}

export function buildBoothNoMap(booths: Booth[]) {
  return new Map(booths.map((booth) => [booth.id, booth.boothNo]));
}

export function formatSelectionBoothNumbers(selection: Pick<BoothSelection, 'boothIds'>, boothNoById: Map<string, string>) {
  if (!selection.boothIds?.length) return '暂无展位信息';
  const boothNumbers = selection.boothIds.map((boothId) => boothNoById.get(boothId) ?? formatShortId(boothId));
  return boothNumbers.length ? boothNumbers.join('、') : '暂无展位信息';
}

export function formatNullableAdminText(value?: string | null, fallback = '暂无') {
  const trimmed = value?.trim();
  return trimmed ? trimmed : fallback;
}
