import { ModalForm, PageContainer, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Space } from 'antd';
import { useRef, useState } from 'react';
import { notifyAdminDashboardStale } from '../../../app/apiLoaders';
import { formatEventDisplay, formatExhibitorDisplay, formatPaymentMethod } from '../../../app/display';
import type { ParticipationOrder, PaymentRecord } from '../../../app/domain';
import { formatFriendlyTime, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { getPaymentStatusMeta, StatusTag } from '../../../app/status';
import { useEvents } from '../../../app/useEvents';
import { ADMIN_REASON_MAX_LENGTH, buildAdminReasonRules, formatNullableAdminText, joinPaymentsWithOrders } from '../pageLogic';

type PaymentRow = PaymentRecord & {
  exhibitorId?: string | null;
};

export default function PaymentConfirmPage() {
  const actionRef = useRef<ActionType>();
  const [loadingId, setLoadingId] = useState<string>();
  const { events } = useEvents();

  const columns: ProColumns<PaymentRow>[] = [
    { title: '展会', dataIndex: 'eventId', ellipsis: true, render: (_, record) => (record.eventId ? formatEventDisplay(record.eventId, events) : '未填写') },
    { title: '展商', dataIndex: 'exhibitorId', ellipsis: true, render: (_, record) => (record.exhibitorId ? formatExhibitorDisplay(record.exhibitorId) : '未填写') },
    { title: '订单', dataIndex: 'orderId', ellipsis: true, render: (_, record) => formatShortId(record.orderId) },
    { title: '方式', dataIndex: 'method', render: (_, record) => formatPaymentMethod(record.method) },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getPaymentStatusMeta(record.status)} />,
    },
    { title: '金额', dataIndex: 'amount', render: (_, record) => (record.amount === null || record.amount === undefined || record.amount === '' ? '-' : formatMoney(record.amount)) },
    {
      title: '失败原因',
      dataIndex: 'failureReason',
      render: (_, record) => (record.status === 'CONFIRM_FAILED' ? formatNullableAdminText(record.failureReason, '暂无失败原因') : null),
    },
    { title: '创建时间', dataIndex: 'createdAt', render: (_, record) => formatFriendlyTime(record.createdAt) },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => {
        if (record.status !== 'PENDING_FINANCE_CONFIRM') return null;
        return (
          <Space size={8}>
            <Button
              type="link"
              size="small"
              loading={loadingId === record.id}
              onClick={() => {
                Modal.confirm({
                  title: '确认付款已到账？',
                  content: `付款 ${formatShortId(record.id)} 金额 ${record.amount === null || record.amount === undefined || record.amount === '' ? '-' : formatMoney(record.amount)}，确认后订单付款状态将更新。`,
                  okText: '确认到账',
                  cancelText: '取消',
                  onOk: async () => {
                    setLoadingId(record.id);
                    try {
                      await apiPost<PaymentRow>(`/api/payments/${record.id}/confirm`);
                      message.success('付款已确认到账');
                      notifyAdminDashboardStale();
                      actionRef.current?.reload();
                    } finally {
                      setLoadingId(undefined);
                    }
                  },
                });
              }}
            >
              确认到账
            </Button>
            <ModalForm<{ reason: string }>
              title="确认付款失败"
              trigger={
                <Button type="link" size="small" danger loading={loadingId === record.id}>
                  确认失败
                </Button>
              }
              modalProps={{ destroyOnClose: true }}
              submitter={{
                submitButtonProps: { danger: true, loading: loadingId === record.id },
                searchConfig: { submitText: '确认失败', resetText: '取消' },
              }}
              onFinish={async (values) => {
                setLoadingId(record.id);
                try {
                  await apiPost<PaymentRow, { reason: string }>(`/api/payments/${record.id}/fail`, { reason: values.reason });
                  message.success('已确认失败');
                  notifyAdminDashboardStale();
                  actionRef.current?.reload();
                  return true;
                } finally {
                  setLoadingId(undefined);
                }
              }}
            >
              <ProFormTextArea
                name="reason"
                label="失败原因"
                rules={buildAdminReasonRules('请输入失败原因')}
                fieldProps={{ showCount: true, maxLength: ADMIN_REASON_MAX_LENGTH }}
              />
            </ModalForm>
          </Space>
        );
      },
    },
  ];

  return (
    <PageContainer title="付款确认">
      <ProTable<PaymentRow>
        actionRef={actionRef}
        rowKey="id"
        search={false}
        columns={columns}
        locale={{ emptyText: '暂无付款记录' }}
        request={async () => {
          const [payments, orders] = await Promise.all([
            apiGet<PaymentRecord[]>('/api/payments'),
            apiGet<ParticipationOrder[]>('/api/participation-orders'),
          ]);
          const data = joinPaymentsWithOrders(payments, orders);
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
