import type { AdminDashboardResponse, SelectionEligibility } from './domain';
import { apiGet } from './request';

export const ADMIN_DASHBOARD_REFRESH_EVENT = 'admin-dashboard-refresh';

export async function loadSelectionEligibility(orderId: string): Promise<SelectionEligibility> {
  return apiGet<SelectionEligibility>(`/api/selection-eligibilities?orderId=${encodeURIComponent(orderId)}`);
}

export async function loadAdminDashboard(): Promise<AdminDashboardResponse> {
  return apiGet<AdminDashboardResponse>('/api/admin/dashboard');
}

export function notifyAdminDashboardStale() {
  window.dispatchEvent(new Event(ADMIN_DASHBOARD_REFRESH_EVENT));
}
