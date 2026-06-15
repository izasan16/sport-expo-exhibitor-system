import type { AdminDashboardResponse, ParticipationOrder } from '../../../app/domain';
import { formatFriendlyTime } from '../../../app/formatters';

export type AdminDashboardKpi = {
  title: string;
  value: number;
  to?: string;
};

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

export function buildAdminDashboardKpis(response: AdminDashboardResponse): AdminDashboardKpi[] {
  return [
    {
      title: '待审核报名',
      value: response.pendingRegistrationReviewCount ?? 0,
      to: '/admin/registrations?status=SUBMITTED',
    },
    {
      title: '待销售确认',
      value: response.pendingSalesConfirmCount ?? 0,
      to: '/admin/orders?status=PENDING_SALES_CONFIRM',
    },
    { title: '今日新增订单', value: response.todayNewOrderCount ?? 0 },
    {
      title: '待财务确认',
      value: response.pendingFinanceConfirmCount ?? 0,
      to: '/admin/payments',
    },
    {
      title: '待选位审核',
      value: response.pendingSelectionReviewCount ?? 0,
      to: '/admin/selections',
    },
  ];
}

export function buildDashboardViewModel(response: AdminDashboardResponse, now = new Date()) {
  return {
    generatedAtDisplay: formatDashboardGeneratedAt(response.generatedAt, now),
    kpis: buildAdminDashboardKpis(response),
    counts: {
      pendingRegistrationReview: response.pendingRegistrationReviewCount ?? 0,
      pendingSalesConfirm: response.pendingSalesConfirmCount ?? 0,
      pendingFinanceConfirm: response.pendingFinanceConfirmCount ?? 0,
      pendingSelectionReview: response.pendingSelectionReviewCount ?? 0,
      todayNewOrder: response.todayNewOrderCount ?? 0,
    },
    pendingRegistrationReviews: response.pendingRegistrationReviews ?? [],
    pendingSalesConfirmOrders: response.pendingSalesConfirmOrders ?? [],
    pendingFinanceConfirmPayments: response.pendingFinanceConfirmPayments ?? [],
    pendingSelectionReviews: response.pendingSelectionReviews ?? [],
  };
}
