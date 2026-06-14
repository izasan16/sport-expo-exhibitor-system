import type { AdminDashboardResponse, ParticipationOrder } from '../../../app/domain';
import { formatFriendlyTime } from '../../../app/formatters';

export function countTodayOrders(orders: ParticipationOrder[], now = new Date()) {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const end = start + 24 * 60 * 60 * 1000;
  return orders.filter((order) => {
    const created = Date.parse(order.createdAt);
    return created >= start && created < end;
  }).length;
}

export function formatDashboardGeneratedAt(generatedAt?: string | null, now = new Date()) {
  return generatedAt ? formatFriendlyTime(generatedAt, now) : '时间未记录';
}

export function buildDashboardViewModel(response: AdminDashboardResponse, now = new Date()) {
  return {
    generatedAtDisplay: formatDashboardGeneratedAt(response.generatedAt, now),
    counts: {
      pendingSalesConfirm: response.pendingSalesConfirmCount ?? 0,
      pendingFinanceConfirm: response.pendingFinanceConfirmCount ?? 0,
      pendingSelectionReview: response.pendingSelectionReviewCount ?? 0,
      todayNewOrder: response.todayNewOrderCount ?? 0,
    },
    pendingSalesConfirmOrders: response.pendingSalesConfirmOrders ?? [],
    pendingFinanceConfirmPayments: response.pendingFinanceConfirmPayments ?? [],
    pendingSelectionReviews: response.pendingSelectionReviews ?? [],
  };
}
