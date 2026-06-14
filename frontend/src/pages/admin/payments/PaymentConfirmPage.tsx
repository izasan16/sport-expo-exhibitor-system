import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Space } from 'antd';
import { useRef, useState } from 'react';
import { formatFriendlyTime, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { getPaymentStatusMeta, StatusTag } from '../../../app/status';

type PaymentRow = {
  id: string;
  eventId: string;
  orderId: string;
  amount: number;
  status: string;
  method: string;
  confirmedAt?: string;
  createdAt: string;
  updatedAt: string;
};

export default function PaymentConfirmPage() {
  const actionRef = useRef<ActionType>();
  const [loadingId, setLoadingId] = useState<string>();

  const columns: ProColumns<PaymentRow>[] = [
    { title: '订单', dataIndex: 'orderId', ellipsis: true, render: (_, record) => formatShortId(record.orderId) },
    { title: '展会', dataIndex: 'eventId', ellipsis: true, render: (_, record) => formatShortId(record.eventId) },
    { title: '金额', dataIndex: 'amount', render: (_, record) => formatMoney(record.amount) },
    { title: '方式', dataIndex: 'method' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getPaymentStatusMeta(record.status)} />,
    },
    { title: '创建时间', dataIndex: 'createdAt', render: (_, record) => formatFriendlyTime(record.createdAt) },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            loading={loadingId === record.id}
            onClick={() => {
              Modal.confirm({
                title: '确认付款已到账？',
                content: `付款 ${formatShortId(record.id)} 金额 ${formatMoney(record.amount)}，确认后订单付款状态将更新。`,
                okText: '确认到账',
                cancelText: '取消',
                onOk: async () => {
                  setLoadingId(record.id);
                  try {
                    await apiPost<PaymentRow>(`/api/payments/${record.id}/confirm`);
                    message.success('付款已确认到账');
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
          <Button
            type="link"
            size="small"
            danger
            loading={loadingId === record.id}
            onClick={() => {
              Modal.confirm({
                title: '确认付款失败？',
                content: `付款 ${formatShortId(record.id)} 将标记为确认失败，请确认已核对付款凭证。`,
                okText: '确认失败',
                okButtonProps: { danger: true },
                cancelText: '取消',
                onOk: async () => {
                  setLoadingId(record.id);
                  try {
                    await apiPost<PaymentRow>(`/api/payments/${record.id}/fail`);
                    message.success('已确认失败');
                    actionRef.current?.reload();
                  } finally {
                    setLoadingId(undefined);
                  }
                },
              });
            }}
          >
            确认失败
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="付款确认">
      <ProTable<PaymentRow>
        actionRef={actionRef}
        rowKey="id"
        search={false}
        columns={columns}
        locale={{ emptyText: '当前没有待确认付款' }}
        request={async () => {
          const data = await apiGet<PaymentRow[]>('/api/payments?status=PENDING_FINANCE_CONFIRM');
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
