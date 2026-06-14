import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Button, message, Space } from 'antd';
import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import type { ParticipationOrder } from '../../../app/domain';
import { formatArea, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet } from '../../../app/request';
import { getOrderStatusMeta, StatusTag } from '../../../app/status';
import { getOrderNextAction } from '../pageLogic';

export default function MyOrdersPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const messageShownRef = useRef(false);

  useEffect(() => {
    const state = location.state as { message?: string } | null;
    if (!messageShownRef.current && state?.message) {
      messageShownRef.current = true;
      message.info(state.message);
    }
  }, [location.state]);

  const columns: ProColumns<ParticipationOrder>[] = [
    {
      title: '订单',
      dataIndex: 'id',
      ellipsis: true,
      render: (_, record) => formatShortId(record.id),
    },
    { title: '展会 ID', dataIndex: 'eventId', ellipsis: true },
    {
      title: '面积',
      dataIndex: 'areaSqm',
      render: (_, record) => formatArea(record.areaSqm),
    },
    { title: '展位类型', dataIndex: 'boothType' },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      render: (_, record) => formatMoney(record.totalAmount),
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getOrderStatusMeta(record.status)} />,
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => {
        const nextAction = getOrderNextAction({ order: record });
        return (
          <Space size={8}>
            <Button type="link" size="small" onClick={() => navigate(`/exhibitor/orders/${record.id}`)}>
              查看详情
            </Button>
            <Button
              type="link"
              size="small"
              disabled={nextAction.disabled}
              onClick={() => navigate(nextAction.to)}
            >
              {nextAction.label}
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <PageContainer title="我的订单">
      <ProTable<ParticipationOrder>
        rowKey="id"
        search={false}
        columns={columns}
        locale={{ emptyText: '暂无参展订单' }}
        request={async () => {
          const data = await apiGet<ParticipationOrder[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`);
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
