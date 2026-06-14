import { describe, expect, it } from 'vitest';
import {
  ADMIN_REASON_MAX_LENGTH,
  buildBatchConfirmFailureItems,
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
});
