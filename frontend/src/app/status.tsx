import { Tag } from 'antd';
import type { BoothStatus, ContractSignStatus, ContractStatus, EligibilityStatus, OrderStatus, PaymentStatus, SelectionStatus } from './domain';

type StatusMeta = { label: string; color: string };

const orderStatusMap: Record<OrderStatus, StatusMeta> = {
  PENDING_SALES_CONFIRM: { label: '待销售确认', color: 'gold' },
  PENDING_CONTRACT: { label: '待生成合同', color: 'blue' },
  PENDING_PAYMENT: { label: '待付款', color: 'orange' },
  PARTIALLY_PAID: { label: '部分付款', color: 'orange' },
  PAID: { label: '已付款', color: 'green' },
  COMPLETED: { label: '已完成', color: 'default' },
};

const paymentStatusMap: Record<PaymentStatus, StatusMeta> = {
  PENDING_FINANCE_CONFIRM: { label: '待财务确认', color: 'orange' },
  CONFIRMED: { label: '已确认到账', color: 'green' },
  CONFIRM_FAILED: { label: '确认失败', color: 'red' },
};

const contractStatusMap: Record<ContractStatus | ContractSignStatus, StatusMeta> = {
  GENERATED: { label: '已生成', color: 'blue' },
  EFFECTIVE: { label: '已生效', color: 'green' },
  UNSIGNED: { label: '待线下签署', color: 'orange' },
  MARKED_SIGNED: { label: '已标记签署', color: 'green' },
};

const selectionStatusMap: Record<SelectionStatus, StatusMeta> = {
  PENDING_REVIEW: { label: '待审核', color: 'orange' },
  CONFIRMED: { label: '已确认', color: 'green' },
  REJECTED: { label: '已驳回', color: 'red' },
  TIMEOUT: { label: '已超时释放', color: 'default' },
};

const boothStatusMap: Record<BoothStatus, StatusMeta> = {
  AVAILABLE: { label: '可选', color: 'green' },
  LOCKED: { label: '锁定中', color: 'orange' },
  CONFIRMED: { label: '已确认', color: 'blue' },
  RESERVED: { label: '预留', color: 'purple' },
  DISABLED: { label: '禁用', color: 'default' },
};

const eligibilityStatusMap: Record<EligibilityStatus, StatusMeta> = {
  NOT_OPEN: { label: '未开放', color: 'default' },
  AVAILABLE: { label: '可选位', color: 'green' },
  PAUSED: { label: '暂停中', color: 'orange' },
  CLOSED: { label: '已关闭', color: 'blue' },
};

function fallbackMeta(status: string): StatusMeta {
  return { label: status, color: 'default' };
}

export function getOrderStatusMeta(status: OrderStatus | string) {
  return orderStatusMap[status as OrderStatus] ?? fallbackMeta(status);
}

export function getPaymentStatusMeta(status: PaymentStatus | string) {
  return paymentStatusMap[status as PaymentStatus] ?? fallbackMeta(status);
}

export function getContractStatusMeta(status: ContractStatus | ContractSignStatus | string) {
  return contractStatusMap[status as ContractStatus | ContractSignStatus] ?? fallbackMeta(status);
}

export function getSelectionStatusMeta(status: SelectionStatus | string) {
  return selectionStatusMap[status as SelectionStatus] ?? fallbackMeta(status);
}

export function getBoothStatusMeta(status: BoothStatus | string) {
  return boothStatusMap[status as BoothStatus] ?? fallbackMeta(status);
}

export function getEligibilityStatusMeta(status: EligibilityStatus | string) {
  return eligibilityStatusMap[status as EligibilityStatus] ?? fallbackMeta(status);
}

export function StatusTag({ meta }: { meta: StatusMeta }) {
  return <Tag color={meta.color}>{meta.label}</Tag>;
}
