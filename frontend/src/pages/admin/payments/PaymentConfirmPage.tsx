import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Space } from 'antd';
import { useRef } from 'react';
import { apiGet, apiPost } from '../../../app/request';

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

  const columns: ProColumns<PaymentRow>[] = [
    { title: '订单 ID', dataIndex: 'orderId', ellipsis: true },
    { title: '展会 ID', dataIndex: 'eventId', ellipsis: true },
    { title: '金额', dataIndex: 'amount', valueType: 'money' },
    { title: '方式', dataIndex: 'method' },
    { title: '状态', dataIndex: 'status' },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime' },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await apiPost<PaymentRow>(`/api/payments/${record.id}/confirm`);
              message.success('付款已确认到账');
              actionRef.current?.reload();
            }}
          >
            确认到账
          </Button>
          <Button
            type="link"
            size="small"
            danger
            onClick={async () => {
              await apiPost<PaymentRow>(`/api/payments/${record.id}/fail`);
              message.success('已确认失败');
              actionRef.current?.reload();
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
        request={async () => {
          const data = await apiGet<PaymentRow[]>('/api/payments?status=PENDING_FINANCE_CONFIRM');
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
