import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Button, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { apiGet } from '../../../app/request';
import { isOrderPayable, isOrderSelectable } from '../pageLogic';

type OrderRow = {
  id: string;
  eventId: string;
  areaSqm: number | string;
  boothType: string;
  totalAmount: number | string;
  status: string;
};

export default function MyOrdersPage() {
  const navigate = useNavigate();

  const columns: ProColumns<OrderRow>[] = [
    { title: '展会 ID', dataIndex: 'eventId', ellipsis: true },
    { title: '面积', dataIndex: 'areaSqm', valueType: 'digit' },
    { title: '展位类型', dataIndex: 'boothType' },
    { title: '总金额', dataIndex: 'totalAmount', valueType: 'money' },
    { title: '状态', dataIndex: 'status' },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            disabled={!isOrderPayable(record.status)}
            onClick={() => navigate(`/exhibitor/payments/new?orderId=${encodeURIComponent(record.id)}`)}
          >
            提交付款
          </Button>
          <Button
            type="link"
            size="small"
            disabled={!isOrderSelectable(record.status)}
            onClick={() => navigate(`/exhibitor/selections/new?orderId=${encodeURIComponent(record.id)}`)}
          >
            选择展位
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="我的订单">
      <ProTable<OrderRow>
        rowKey="id"
        search={false}
        columns={columns}
        request={async () => {
          const data = await apiGet<OrderRow[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`);
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
