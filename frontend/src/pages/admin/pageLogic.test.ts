import { describe, expect, it } from 'vitest';
import {
  ADMIN_REASON_MAX_LENGTH,
  buildBatchConfirmFailureItems,
  filterConfirmableSelectedKeys,
  formatNullableAdminText,
  formatRoleLabels,
  validateRequiredAdminReason,
} from './pageLogic';
import type { BatchConfirmOrdersResponse } from '../../app/domain';

describe('admin page logic', () => {
  it('summarizes batch confirm failures with short order ids and reasons', () => {
    const response: BatchConfirmOrdersResponse = {
      successCount: 1,
      failureCount: 2,
      failures: [
        { orderId: 'e0e5589c-90aa-43b5-beb0-c42878beb4aa', reason: '订单状态已变化' },
        { orderId: 'abc12345-90aa-43b5-beb0-c42878beb4aa', reason: 'Order confirmation failed' },
      ],
    };

    expect(buildBatchConfirmFailureItems(response)).toEqual([
      { key: 'e0e5589c-90aa-43b5-beb0-c42878beb4aa', text: 'e0e5589c：订单状态已变化' },
      { key: 'abc12345-90aa-43b5-beb0-c42878beb4aa', text: 'abc12345：Order confirmation failed' },
    ]);
  });

  it('validates required reject reasons within the shared admin reason length limit', () => {
    expect(validateRequiredAdminReason('')).toBe('请输入原因');
    expect(validateRequiredAdminReason('   ')).toBe('请输入原因');
    expect(validateRequiredAdminReason('a'.repeat(ADMIN_REASON_MAX_LENGTH))).toBeUndefined();
    expect(validateRequiredAdminReason('a'.repeat(ADMIN_REASON_MAX_LENGTH + 1))).toBe('原因不能超过 500 字');
  });

  it('trims nullable admin display text before rendering it', () => {
    expect(formatNullableAdminText('  推荐 A101  ')).toBe('推荐 A101');
    expect(formatNullableAdminText('   ')).toBe('暂无');
    expect(formatNullableAdminText('DISABLED')).toBe('DISABLED');
  });

  it('joins multiple role labels in stable order', () => {
    expect(formatRoleLabels(['SALES', 'ORGANIZER_ADMIN'])).toBe('主办方管理员 / 销售');
  });

  it('keeps batch confirm ids limited to visible confirmable rows', () => {
    const currentRows = [
      { id: 'visible-confirmable', status: 'PENDING_SALES_CONFIRM' },
      { id: 'visible-not-confirmable', status: 'PENDING_PAYMENT' },
    ];

    expect(filterConfirmableSelectedKeys(
      ['visible-confirmable', 'visible-not-confirmable', 'hidden-confirmable'],
      currentRows,
    )).toEqual(['visible-confirmable']);
  });
});
