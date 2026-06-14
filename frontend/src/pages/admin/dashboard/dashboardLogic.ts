import type { ParticipationOrder } from '../../../app/domain';

export function countTodayOrders(orders: ParticipationOrder[], now = new Date()) {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const end = start + 24 * 60 * 60 * 1000;
  return orders.filter((order) => {
    const created = Date.parse(order.createdAt);
    return created >= start && created < end;
  }).length;
}
