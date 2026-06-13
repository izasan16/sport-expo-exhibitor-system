import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Space } from 'antd';
import { useRef } from 'react';
import { apiGet, apiPost } from '../../../app/request';

type OrderRow = {
  id: string;
  eventId: string;
  exhibitorId: string;
  zoneId?: string;
  areaTierId?: string;
  areaSqm: number;
  boothType: string;
  status: string;
  areaFee: number;
  serviceFee: number;
  discountAmount: number;
  adjustmentAmount: number;
  totalAmount: number;
};

function buildQuery(params: Record<string, unknown>) {
  const searchParams = new URLSearchParams();
  ['eventId', 'exhibitorId', 'status'].forEach((key) => {
    const value = params[key];
    if (value) {
      searchParams.set(key, String(value));
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

export default function AdminOrderListPage() {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<OrderRow>[] = [
    { title: '展会 ID', dataIndex: 'eventId', ellipsis: true },
    { title: '展商 ID', dataIndex: 'exhibitorId', ellipsis: true },
    { title: '状态', dataIndex: 'status' },
    { title: '展位类型', dataIndex: 'boothType', hideInSearch: true },
    { title: '面积', dataIndex: 'areaSqm', valueType: 'digit', hideInSearch: true },
    { title: '总金额', dataIndex: 'totalAmount', valueType: 'money', hideInSearch: true },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await apiPost<OrderRow>(`/api/participation-orders/${record.id}/confirm`);
              message.success('销售确认已完成');
              actionRef.current?.reload();
            }}
          >
            销售确认
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="订单管理">
      <ProTable<OrderRow>
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        columns={columns}
        request={async (params) => {
          const data = await apiGet<OrderRow[]>(`/api/participation-orders${buildQuery(params)}`);
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
