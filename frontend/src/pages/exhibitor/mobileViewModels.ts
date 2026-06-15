import { formatArea, formatFriendlyTime, formatMoney, formatShortId } from '../../app/formatters';
export { formatShortId } from '../../app/formatters';

export type NotificationType =
  | 'REGISTRATION_SUBMITTED'
  | 'REGISTRATION_APPROVED'
  | 'REGISTRATION_REJECTED'
  | 'ORDER_SALES_CONFIRMED'
  | 'CONTRACT_SIGNED'
  | 'PAYMENT_CONFIRMED'
  | 'PAYMENT_FAILED'
  | 'SELECTION_SUBMITTED'
  | 'SELECTION_APPROVED'
  | 'SELECTION_REJECTED'
  | 'SELECTION_TIMEOUT_RELEASED';

export type FlowStep = {
  title: string;
};

export type HomeWorkbenchSection =
  | 'dashboardSummary'
  | 'todoList'
  | 'flow'
  | 'recentOrders'
  | 'emptyFlow'
  | 'startOrder';

export type HomeDashboardOrderSource = {
  id: string;
  eventName?: string | null;
  status?: string | null;
  totalAmount?: string | number | null;
  areaSqm?: string | number | null;
  updatedAt?: string | null;
};

export type HomeDashboardRelatedStatus = {
  status?: string | null;
};

export type HomeDashboardInput = {
  orders: HomeDashboardOrderSource[];
  eligibilitiesByOrderId?: Record<string, HomeDashboardRelatedStatus | undefined>;
  latestSelectionsByOrderId?: Record<string, HomeDashboardRelatedStatus | undefined>;
};

export type HomeDashboardSummary = {
  totalCount: number;
  pendingPaymentCount: number;
  readyForSelectionCount: number;
  selectionReviewCount: number;
  completedCount: number;
  otherInProgressCount: number;
};

export type HomeTodoItem = {
  key: string;
  orderId: string;
  priority: number;
  title: string;
  description: string;
  actionLabel: string;
  to: string;
  disabled: boolean;
};

export type OrderContextHeaderInput = {
  orderId?: string | null;
  eventName?: string | null;
  totalAmount?: string | number | null;
  areaSqm?: string | number | null;
  boothTypeLabel?: string | null;
};

export type OrderContextHeader = {
  title: string;
  subtitle: string;
  metrics: Array<{ label: string; value: string }>;
};

export type SelectedBoothSource = {
  id: string;
  boothNo: string;
  areaSqm: number | string;
};

export type SelectedBoothSummary = {
  booths: SelectedBoothSource[];
  boothIds: string[];
  selectedCount: number;
  selectedAreaSqm: number;
  selectedAreaDisplay: string;
};

type TimelineEntity = {
  id: string;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type MobileTimelineInput = {
  order?: TimelineEntity;
  payments?: Array<TimelineEntity & { confirmedAt?: string | null }>;
  selections?: Array<TimelineEntity & { reviewedAt?: string | null }>;
  contracts?: Array<TimelineEntity & { signedAt?: string | null }>;
};

export type MobileTimelineEvent = {
  key: string;
  title: string;
  happenedAt: string;
  timeDisplay: string;
};

export type PaymentProgressSource = {
  amount?: string | number | null;
  status?: string | null;
};

export type PaymentProgressSummary = {
  confirmedAmount: number;
  totalAmount: number;
  remainingAmount: number;
  percent: number;
  confirmedDisplay: string;
  totalDisplay: string;
  remainingDisplay: string;
};

export type NotificationTargetSource = {
  type?: string | null;
  relatedType?: string | null;
  relatedId?: string | null;
  relatedOrderId?: string | null;
};

const homeFlowSteps: FlowStep[] = [
  { title: '提交订单' },
  { title: '销售确认' },
  { title: '合同确认' },
  { title: '付款确认' },
  { title: '选择展位' },
  { title: '审核完成' },
];

const notificationTypeLabels: Record<NotificationType, string> = {
  REGISTRATION_SUBMITTED: '报名已提交',
  REGISTRATION_APPROVED: '报名已通过',
  REGISTRATION_REJECTED: '报名已驳回',
  ORDER_SALES_CONFIRMED: '销售已确认',
  CONTRACT_SIGNED: '合同已签署',
  PAYMENT_CONFIRMED: '付款已确认',
  PAYMENT_FAILED: '付款确认失败',
  SELECTION_SUBMITTED: '选位已提交',
  SELECTION_APPROVED: '选位已通过',
  SELECTION_REJECTED: '选位已驳回',
  SELECTION_TIMEOUT_RELEASED: '选位超时释放',
};

function toNumber(value: string | number | undefined | null) {
  if (value === undefined || value === null || value === '') {
    return 0;
  }
  return typeof value === 'number' ? value : Number(value);
}

function addTimelineEvent(events: MobileTimelineEvent[], key: string, title: string, happenedAt?: string | null) {
  if (!happenedAt) {
    return;
  }

  events.push({
    key,
    title,
    happenedAt,
    timeDisplay: formatMobileTime(happenedAt),
  });
}

function addTimelineEventWithFallback(
  events: MobileTimelineEvent[],
  key: string,
  completedTitle: string,
  fallbackTitle: string,
  completedAt?: string | null,
  fallbackAt?: string | null,
) {
  if (completedAt) {
    addTimelineEvent(events, key, completedTitle, completedAt);
    return;
  }

  addTimelineEvent(events, key, fallbackTitle, fallbackAt);
}

export function buildHomeFlowSteps() {
  return [...homeFlowSteps];
}

export function buildHomeWorkbenchSections(hasActiveOrder: boolean): HomeWorkbenchSection[] {
  return hasActiveOrder
    ? ['dashboardSummary', 'todoList', 'flow', 'recentOrders']
    : ['emptyFlow', 'startOrder'];
}

function isPendingPaymentStatus(status?: string | null) {
  return status === 'PENDING_PAYMENT' || status === 'PARTIALLY_PAID';
}

function isReadyForSelection(order: HomeDashboardOrderSource, eligibility?: HomeDashboardRelatedStatus) {
  return order.status === 'PAID' && eligibility?.status === 'AVAILABLE';
}

function isSelectionReviewing(selection?: HomeDashboardRelatedStatus) {
  return selection?.status === 'PENDING_REVIEW';
}

function buildTodoDescription(order: HomeDashboardOrderSource) {
  return `${formatMobileText(order.eventName)} · 订单 ${formatShortId(order.id)} · ${formatMobileAmount(order.totalAmount)} · ${formatArea(order.areaSqm)}`;
}

export function buildHomeDashboardSummary(input: HomeDashboardInput): HomeDashboardSummary {
  const pendingPaymentCount = input.orders.filter((order) => isPendingPaymentStatus(order.status)).length;
  const readyForSelectionCount = input.orders.filter((order) => (
    isReadyForSelection(order, input.eligibilitiesByOrderId?.[order.id])
  )).length;
  const selectionReviewCount = input.orders.filter((order) => (
    isSelectionReviewing(input.latestSelectionsByOrderId?.[order.id])
  )).length;
  const completedCount = input.orders.filter((order) => order.status === 'COMPLETED').length;
  const categorizedCount = pendingPaymentCount + readyForSelectionCount + selectionReviewCount + completedCount;

  return {
    totalCount: input.orders.length,
    pendingPaymentCount,
    readyForSelectionCount,
    selectionReviewCount,
    completedCount,
    otherInProgressCount: Math.max(input.orders.length - categorizedCount, 0),
  };
}

export function buildHomeTodoItems(input: HomeDashboardInput): HomeTodoItem[] {
  return input.orders
    .flatMap<HomeTodoItem>((order) => {
      const eligibility = input.eligibilitiesByOrderId?.[order.id];
      const latestSelection = input.latestSelectionsByOrderId?.[order.id];
      const description = buildTodoDescription(order);

      if (isPendingPaymentStatus(order.status)) {
        return [{
          key: `${order.id}-payment`,
          orderId: order.id,
          priority: 10,
          title: '等待您提交付款',
          description,
          actionLabel: '去付款',
          to: `/exhibitor/orders/${order.id}/payment`,
          disabled: false,
        }];
      }

      if (isReadyForSelection(order, eligibility)) {
        return [{
          key: `${order.id}-selection`,
          orderId: order.id,
          priority: 20,
          title: '可以选择展位了',
          description,
          actionLabel: '去选位',
          to: `/exhibitor/orders/${order.id}/selection`,
          disabled: false,
        }];
      }

      if (isSelectionReviewing(latestSelection)) {
        return [{
          key: `${order.id}-selection-review`,
          orderId: order.id,
          priority: 30,
          title: '选位正在审核',
          description,
          actionLabel: '看进度',
          to: `/exhibitor/orders/${order.id}`,
          disabled: false,
        }];
      }

      if (order.status === 'PENDING_SALES_CONFIRM') {
        return [{
          key: `${order.id}-sales-confirm`,
          orderId: order.id,
          priority: 40,
          title: '等待销售确认',
          description,
          actionLabel: '看进度',
          to: `/exhibitor/orders/${order.id}`,
          disabled: false,
        }];
      }

      if (order.status === 'PENDING_CONTRACT') {
        return [{
          key: `${order.id}-contract`,
          orderId: order.id,
          priority: 50,
          title: '等待合同确认',
          description,
          actionLabel: '看进度',
          to: `/exhibitor/orders/${order.id}`,
          disabled: false,
        }];
      }

      return [];
    })
    .sort((left, right) => left.priority - right.priority);
}

export function buildOrderContextHeader(input: OrderContextHeaderInput): OrderContextHeader {
  return {
    title: formatMobileText(input.eventName),
    subtitle: `订单 ${formatShortId(input.orderId)}`,
    metrics: [
      { label: '订单金额', value: formatMobileAmount(input.totalAmount) },
      { label: '订单面积', value: formatArea(input.areaSqm) },
      { label: '展位类型', value: formatMobileText(input.boothTypeLabel) },
    ],
  };
}

export function buildSelectedBoothSummary(booths: SelectedBoothSource[]): SelectedBoothSummary {
  const selectedArea = booths.reduce((sum, booth) => sum + toNumber(booth.areaSqm), 0);
  const selectedAreaSqm = Number(selectedArea.toFixed(2));

  return {
    booths: [...booths],
    boothIds: booths.map((booth) => booth.id),
    selectedCount: booths.length,
    selectedAreaSqm,
    selectedAreaDisplay: formatArea(selectedAreaSqm),
  };
}

export function summarizeWithAddedBooth(booths: SelectedBoothSource[], booth: SelectedBoothSource) {
  if (booths.some((item) => item.id === booth.id)) {
    return buildSelectedBoothSummary(booths);
  }
  return buildSelectedBoothSummary([...booths, booth]);
}

export function summarizeWithRemovedBooth(booths: SelectedBoothSource[], boothId: string) {
  return buildSelectedBoothSummary(booths.filter((booth) => booth.id !== boothId));
}

export function buildMobileTimelineEvents(input: MobileTimelineInput) {
  const events: MobileTimelineEvent[] = [];

  if (input.order) {
    addTimelineEvent(events, `order-${input.order.id}-created`, '订单已提交', input.order.createdAt);
    addTimelineEvent(events, `order-${input.order.id}-updated`, '订单已更新', input.order.updatedAt);
  }

  input.payments?.forEach((payment) => {
    addTimelineEvent(events, `payment-${payment.id}-created`, '付款已提交', payment.createdAt);
    addTimelineEventWithFallback(
      events,
      `payment-${payment.id}-confirmed`,
      '付款确认',
      '付款记录更新',
      payment.confirmedAt,
      payment.updatedAt,
    );
  });

  input.selections?.forEach((selection) => {
    addTimelineEvent(events, `selection-${selection.id}-created`, '选位已提交', selection.createdAt);
    addTimelineEventWithFallback(
      events,
      `selection-${selection.id}-reviewed`,
      '选位审核',
      '选位记录更新',
      selection.reviewedAt,
      selection.updatedAt,
    );
  });

  input.contracts?.forEach((contract) => {
    addTimelineEvent(events, `contract-${contract.id}-created`, '合同已生成', contract.createdAt);
    addTimelineEventWithFallback(
      events,
      `contract-${contract.id}-signed`,
      '合同签署',
      '合同记录更新',
      contract.signedAt,
      contract.updatedAt,
    );
  });

  return events.sort((left, right) => Date.parse(left.happenedAt) - Date.parse(right.happenedAt));
}

export function buildPaymentProgressSummary(
  totalPayable: string | number | undefined | null,
  payments: PaymentProgressSource[],
): PaymentProgressSummary {
  const totalAmount = toNumber(totalPayable);
  const confirmedAmount = payments
    .filter((payment) => payment.status === 'CONFIRMED')
    .reduce((sum, payment) => sum + toNumber(payment.amount), 0);
  const remainingAmount = Math.max(totalAmount - confirmedAmount, 0);
  const percent = totalAmount > 0 ? Math.min(Math.round((confirmedAmount / totalAmount) * 100), 100) : 0;

  return {
    confirmedAmount: Number(confirmedAmount.toFixed(2)),
    totalAmount: Number(totalAmount.toFixed(2)),
    remainingAmount: Number(remainingAmount.toFixed(2)),
    percent,
    confirmedDisplay: formatMoney(confirmedAmount),
    totalDisplay: formatMobileAmount(totalPayable),
    remainingDisplay: formatMoney(remainingAmount),
  };
}

export function formatNotificationType(type: NotificationType | string) {
  return notificationTypeLabels[type as NotificationType] ?? type;
}

export function getNotificationTarget(notification: NotificationTargetSource) {
  if (notification.relatedType === 'Registration' && notification.relatedId) {
    return `/exhibitor/registrations/${notification.relatedId}`;
  }

  if (notification.relatedOrderId) {
    return `/exhibitor/orders/${notification.relatedOrderId}`;
  }

  return undefined;
}

export function formatMobileText(value?: string | null) {
  return value?.trim() ? value : '未填写';
}

export function formatMobileOptional(value?: string | null) {
  return value?.trim() ? value : '暂无';
}

export function formatMobileTime(value?: string | null) {
  return value ? formatFriendlyTime(value) : '时间未记录';
}

export function formatMobileAmount(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return formatMoney(value);
}
