import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Button, Card, Empty, List, message, Skeleton, Space, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { formatEventDisplay, formatExhibitorDisplay } from '../../../app/display';
import type { ParticipationOrder } from '../../../app/domain';
import { formatArea, formatShortId } from '../../../app/formatters';
import { apiGet } from '../../../app/request';
import { useIsMobile } from '../../../app/responsive';
import { getOrderStatusMeta, StatusTag } from '../../../app/status';
import { useEvents } from '../../../app/useEvents';
import { formatMobileAmount, formatMobileText, formatMobileTime } from '../mobileViewModels';
import { getOrderNextAction } from '../pageLogic';

export default function MyOrdersPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const messageShownRef = useRef(false);
  const { events } = useEvents();
  const isMobile = useIsMobile();
  const [orders, setOrders] = useState<ParticipationOrder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const state = location.state as { message?: string } | null;
    if (!messageShownRef.current && state?.message) {
      messageShownRef.current = true;
      message.info(state.message);
    }
  }, [location.state]);

  useEffect(() => {
    let alive = true;

    async function loadOrders() {
      setLoading(true);
      try {
        const data = await apiGet<ParticipationOrder[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`);
        if (!alive) return;
        setOrders(data);
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadOrders();
    return () => {
      alive = false;
    };
  }, []);

  const columns: ProColumns<ParticipationOrder>[] = [
    {
      title: '订单',
      dataIndex: 'id',
      ellipsis: true,
      render: (_, record) => formatShortId(record.id),
    },
    { title: '展会', dataIndex: 'eventId', render: (_, record) => (record.eventId ? formatEventDisplay(record.eventId, events) : '未填写') },
    { title: '展商', dataIndex: 'exhibitorId', render: (_, record) => (record.exhibitorId ? formatExhibitorDisplay(record.exhibitorId) : '未填写') },
    {
      title: '面积',
      dataIndex: 'areaSqm',
      render: (_, record) => formatArea(record.areaSqm),
    },
    { title: '展位类型', dataIndex: 'boothType', render: (_, record) => formatMobileText(record.boothType) },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      render: (_, record) => formatMobileAmount(record.totalAmount),
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={record.status ? getOrderStatusMeta(record.status) : { label: '未填写', color: 'default' }} />,
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

  if (isMobile) {
    return (
      <PageContainer title="我的订单">
        {loading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : (
          <List<ParticipationOrder>
            dataSource={orders}
            locale={{ emptyText: <Empty description="暂无参展订单" /> }}
            renderItem={(record) => {
              const nextAction = getOrderNextAction({ order: record });
              const eventDisplay = record.eventId ? formatEventDisplay(record.eventId, events) : '未填写';
              const exhibitorDisplay = record.exhibitorId ? formatExhibitorDisplay(record.exhibitorId) : '未填写';
              const statusMeta = record.status ? getOrderStatusMeta(record.status) : { label: '未填写', color: 'default' };

              return (
                <List.Item style={{ paddingInline: 0 }}>
                  <Card style={{ width: '100%' }} bodyStyle={{ padding: 16 }}>
                    <Space direction="vertical" size={12} style={{ width: '100%' }}>
                      <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
                        <Space direction="vertical" size={4}>
                          <Typography.Text strong>{formatMobileText(eventDisplay)}</Typography.Text>
                          <Typography.Text type="secondary">订单 {formatShortId(record.id)}</Typography.Text>
                        </Space>
                        <StatusTag meta={statusMeta} />
                      </Space>
                      <Space direction="vertical" size={4}>
                        <Typography.Text>金额：{formatMobileAmount(record.totalAmount)}</Typography.Text>
                        <Typography.Text>展商：{exhibitorDisplay}</Typography.Text>
                        <Typography.Text>面积：{formatArea(record.areaSqm)}</Typography.Text>
                        <Typography.Text>展位类型：{formatMobileText(record.boothType)}</Typography.Text>
                        <Typography.Text type="secondary">创建时间：{formatMobileTime(record.createdAt)}</Typography.Text>
                      </Space>
                      <Space wrap>
                        <Button
                          type="primary"
                          disabled={nextAction.disabled}
                          onClick={() => navigate(nextAction.to)}
                        >
                          {nextAction.label}
                        </Button>
                        <Button onClick={() => navigate(`/exhibitor/orders/${record.id}`)}>查看详情</Button>
                      </Space>
                    </Space>
                  </Card>
                </List.Item>
              );
            }}
          />
        )}
      </PageContainer>
    );
  }

  return (
    <PageContainer title="我的订单">
      <ProTable<ParticipationOrder>
        rowKey="id"
        search={false}
        columns={columns}
        loading={loading}
        dataSource={orders}
        locale={{ emptyText: '暂无参展订单' }}
      />
    </PageContainer>
  );
}
