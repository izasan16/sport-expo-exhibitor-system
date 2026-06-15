import { describe, expect, it } from 'vitest';
import {
  buildHomeDashboardSummary,
  buildHomeFlowSteps,
  buildHomeTodoItems,
  buildHomeWorkbenchSections,
  buildMobileTimelineEvents,
  buildOrderContextHeader,
  buildPaymentProgressSummary,
  buildSelectedBoothSummary,
  formatMobileAmount,
  formatMobileOptional,
  formatMobileText,
  formatMobileTime,
  formatNotificationType,
  formatShortId,
  summarizeWithAddedBooth,
  summarizeWithRemovedBooth,
} from './mobileViewModels';

describe('exhibitor mobile view models', () => {
  it('returns the six-step home flow for the empty home state', () => {
    expect(buildHomeFlowSteps()).toEqual([
      { title: '提交订单' },
      { title: '销售确认' },
      { title: '合同确认' },
      { title: '付款确认' },
      { title: '选择展位' },
      { title: '审核完成' },
    ]);
  });

  it('keeps the exhibitor home as a workbench instead of an order detail page', () => {
    expect(buildHomeWorkbenchSections(true)).toEqual(['dashboardSummary', 'todoList', 'flow', 'recentOrders']);
    expect(buildHomeWorkbenchSections(false)).toEqual(['emptyFlow', 'startOrder']);
    expect(buildHomeWorkbenchSections(true)).not.toContain('recentPayments');
    expect(buildHomeWorkbenchSections(true)).not.toContain('recentSelections');
  });

  it('summarizes all exhibitor orders on home instead of picking one latest order', () => {
    const orders = [
      { id: 'pay-1', status: 'PENDING_PAYMENT', totalAmount: '21600.00', areaSqm: '18.00' },
      { id: 'select-1', status: 'PAID', totalAmount: '10800.00', areaSqm: '9.00' },
      { id: 'review-1', status: 'PAID', totalAmount: '10800.00', areaSqm: '9.00' },
      { id: 'done-1', status: 'COMPLETED', totalAmount: '10800.00', areaSqm: '9.00' },
    ];

    expect(buildHomeDashboardSummary({
      orders,
      eligibilitiesByOrderId: {
        'select-1': { status: 'AVAILABLE' },
        'review-1': { status: 'PAUSED' },
      },
      latestSelectionsByOrderId: {
        'review-1': { status: 'PENDING_REVIEW' },
      },
    })).toEqual({
      totalCount: 4,
      pendingPaymentCount: 1,
      readyForSelectionCount: 1,
      selectionReviewCount: 1,
      completedCount: 1,
      otherInProgressCount: 0,
    });
  });

  it('builds prioritized home todo items from every order', () => {
    const todos = buildHomeTodoItems({
      orders: [
        {
          id: 'select-1',
          eventName: '2026体博会',
          status: 'PAID',
          totalAmount: '10800.00',
          areaSqm: '9.00',
          updatedAt: '2026-06-14T12:00:00+08:00',
        },
        {
          id: 'pay-1',
          eventName: '2026体博会',
          status: 'PENDING_PAYMENT',
          totalAmount: '21600.00',
          areaSqm: '18.00',
          updatedAt: '2026-06-14T11:00:00+08:00',
        },
      ],
      eligibilitiesByOrderId: {
        'select-1': { status: 'AVAILABLE' },
      },
      latestSelectionsByOrderId: {},
    });

    expect(todos.map((todo) => todo.actionLabel)).toEqual(['去付款', '去选位']);
    expect(todos[0]).toMatchObject({
      orderId: 'pay-1',
      title: '等待您提交付款',
      to: '/exhibitor/orders/pay-1/payment',
      disabled: false,
    });
    expect(todos[1]).toMatchObject({
      orderId: 'select-1',
      title: '可以选择展位了',
      to: '/exhibitor/orders/select-1/selection',
    });
  });

  it('builds order context header copy for payment and selection pages', () => {
    expect(buildOrderContextHeader({
      orderId: 'e0e5589c-90aa-43b5-beb0-c42878beb4aa',
      eventName: '2026体博会',
      totalAmount: '21600.00',
      areaSqm: '18.00',
      boothTypeLabel: '标准展位',
    })).toEqual({
      title: '2026体博会',
      subtitle: '订单 e0e5589c',
      metrics: [
        { label: '订单金额', value: '¥21,600.00' },
        { label: '订单面积', value: '18.00 ㎡' },
        { label: '展位类型', value: '标准展位' },
      ],
    });
  });

  it('summarizes selected booths after adding or removing a booth', () => {
    const selectedBooths = [
      { id: 'booth-1', boothNo: 'A001', areaSqm: '9.00' },
      { id: 'booth-2', boothNo: 'A002', areaSqm: '18.50' },
      { id: 'booth-3', boothNo: 'A003', areaSqm: 12 },
    ];

    expect(summarizeWithAddedBooth(selectedBooths, { id: 'booth-4', boothNo: 'A004', areaSqm: '6.25' })).toMatchObject({
      boothIds: ['booth-1', 'booth-2', 'booth-3', 'booth-4'],
      selectedCount: 4,
      selectedAreaSqm: 45.75,
      selectedAreaDisplay: '45.75 ㎡',
    });

    expect(summarizeWithAddedBooth(selectedBooths, selectedBooths[0])).toMatchObject({
      boothIds: ['booth-1', 'booth-2', 'booth-3'],
      selectedCount: 3,
      selectedAreaSqm: 39.5,
      selectedAreaDisplay: '39.50 ㎡',
    });

    expect(buildSelectedBoothSummary(selectedBooths)).toMatchObject({
      boothIds: ['booth-1', 'booth-2', 'booth-3'],
      selectedCount: 3,
      selectedAreaSqm: 39.5,
      selectedAreaDisplay: '39.50 ㎡',
    });

    expect(summarizeWithRemovedBooth(selectedBooths, 'booth-2')).toMatchObject({
      boothIds: ['booth-1', 'booth-3'],
      selectedCount: 2,
      selectedAreaSqm: 21,
      selectedAreaDisplay: '21.00 ㎡',
    });
  });

  it('uses updatedAt fallback when an exact timeline event time is absent', () => {
    const events = buildMobileTimelineEvents({
      order: {
        id: 'order-1',
        createdAt: '2026-06-14T09:00:00+08:00',
        updatedAt: '2026-06-14T09:30:00+08:00',
      },
      payments: [
        {
          id: 'payment-1',
          createdAt: '2026-06-14T10:00:00+08:00',
          updatedAt: '2026-06-14T10:30:00+08:00',
          confirmedAt: null,
        },
      ],
      selections: [
        {
          id: 'selection-1',
          createdAt: '2026-06-14T11:00:00+08:00',
          updatedAt: '2026-06-14T11:45:00+08:00',
          reviewedAt: null,
        },
      ],
      contracts: [
        {
          id: 'contract-1',
          createdAt: '2026-06-14T12:00:00+08:00',
          updatedAt: '2026-06-14T12:15:00+08:00',
          signedAt: null,
        },
      ],
    });

    expect(events).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          key: 'payment-payment-1-confirmed',
          title: '付款记录更新',
          happenedAt: '2026-06-14T10:30:00+08:00',
        }),
        expect.objectContaining({
          key: 'selection-selection-1-reviewed',
          title: '选位记录更新',
          happenedAt: '2026-06-14T11:45:00+08:00',
        }),
        expect.objectContaining({
          key: 'contract-contract-1-signed',
          title: '合同记录更新',
          happenedAt: '2026-06-14T12:15:00+08:00',
        }),
      ]),
    );
  });

  it('summarizes confirmed payment progress with capped percentage and remaining floor', () => {
    const summary = buildPaymentProgressSummary('1000.00', [
      { amount: '300.00', status: 'CONFIRMED' },
      { amount: 250, status: 'PENDING_FINANCE_CONFIRM' },
      { amount: '900.00', status: 'CONFIRMED' },
    ]);

    expect(summary).toEqual({
      confirmedAmount: 1200,
      totalAmount: 1000,
      remainingAmount: 0,
      percent: 100,
      confirmedDisplay: '¥1,200.00',
      totalDisplay: '¥1,000.00',
      remainingDisplay: '¥0.00',
    });

    expect(buildPaymentProgressSummary(null, [{ amount: '20.00', status: 'CONFIRMED' }])).toMatchObject({
      totalAmount: 0,
      remainingAmount: 0,
      percent: 0,
      totalDisplay: '-',
    });
  });

  it('labels all supported notification types', () => {
    expect(formatNotificationType('ORDER_SALES_CONFIRMED')).toBe('销售已确认');
    expect(formatNotificationType('CONTRACT_SIGNED')).toBe('合同已签署');
    expect(formatNotificationType('PAYMENT_CONFIRMED')).toBe('付款已确认');
    expect(formatNotificationType('PAYMENT_FAILED')).toBe('付款确认失败');
    expect(formatNotificationType('SELECTION_SUBMITTED')).toBe('选位已提交');
    expect(formatNotificationType('SELECTION_APPROVED')).toBe('选位已通过');
    expect(formatNotificationType('SELECTION_REJECTED')).toBe('选位已驳回');
    expect(formatNotificationType('SELECTION_TIMEOUT_RELEASED')).toBe('选位超时释放');
  });

  it('formats short ids safely at boundaries', () => {
    expect(formatShortId(undefined)).toBe('-');
    expect(formatShortId(null)).toBe('-');
    expect(formatShortId('e0e5589c-90aa-43b5-beb0-c42878beb4aa')).toBe('e0e5589c');
    expect(formatShortId('short')).toBe('short');
  });

  it('uses null-safe display fallbacks by field type', () => {
    expect(formatMobileText(null)).toBe('未填写');
    expect(formatMobileOptional(undefined)).toBe('暂无');
    expect(formatMobileTime(null)).toBe('时间未记录');
    expect(formatMobileAmount(undefined)).toBe('-');
  });
});
