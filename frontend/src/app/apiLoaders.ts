import type { SelectionEligibility } from './domain';
import { apiGet } from './request';

export async function loadSelectionEligibility(orderId: string): Promise<SelectionEligibility> {
  return apiGet<SelectionEligibility>(`/api/selection-eligibilities?orderId=${encodeURIComponent(orderId)}`);
}
