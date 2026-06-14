import { formatArea, formatFriendlyTime, formatMoney } from '../../app/formatters';
export { formatShortId } from '../../app/formatters';

export type NotificationType =
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

const homeFlowSteps: FlowStep[] = [
  { title: '提交订单' },
  { title: '销售确认' },
  { title: '合同确认' },
  { title: '付款确认' },
  { title: '选择展位' },
  { title: '审核完成' },
];

const notificationTypeLabels: Record<NotificationType, string> = {
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

export function formatNotificationType(type: NotificationType | string) {
  return notificationTypeLabels[type as NotificationType] ?? type;
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
