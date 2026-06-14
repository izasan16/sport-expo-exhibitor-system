import { PageContainer, ProCard, ProList } from '@ant-design/pro-components';
import { Alert, Button, Col, Row, Skeleton, Space, Statistic, Typography } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { BoothSelection, ParticipationOrder, PaymentRecord } from '../../app/domain';
import { formatArea, formatMoney, formatShortId } from '../../app/formatters';
import { apiGet } from '../../app/request';
import { getOrderStatusMeta, getPaymentStatusMeta, getSelectionStatusMeta, StatusTag } from '../../app/status';
import { countTodayOrders } from './dashboard/dashboardLogic';

function byCreatedAtDesc<T extends { createdAt: string }>(left: T, right: T) {
  return Date.parse(right.createdAt) - Date.parse(left.createdAt);
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const requestSeq = useRef(0);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [pendingOrders, setPendingOrders] = useState<ParticipationOrder[]>([]);
  const [allOrders, setAllOrders] = useState<ParticipationOrder[]>([]);
  const [pendingPayments, setPendingPayments] = useState<PaymentRecord[]>([]);
  const [pendingSelections, setPendingSelections] = useState<BoothSelection[]>([]);

  const loadDashboard = useCallback(async () => {
    const requestId = requestSeq.current + 1;
    requestSeq.current = requestId;
    setLoading(true);
    setLoadFailed(false);

    try {
      const [loadedPendingOrders, loadedAllOrders, loadedPendingPayments, loadedPendingSelections] = await Promise.all([
        apiGet<ParticipationOrder[]>('/api/participation-orders?status=PENDING_SALES_CONFIRM'),
        apiGet<ParticipationOrder[]>('/api/participation-orders'),
        apiGet<PaymentRecord[]>('/api/payments?status=PENDING_FINANCE_CONFIRM'),
        apiGet<BoothSelection[]>('/api/booth-selections?status=PENDING_REVIEW'),
      ]);

      if (requestSeq.current !== requestId) return;
      setPendingOrders([...loadedPendingOrders].sort(byCreatedAtDesc));
      setAllOrders([...loadedAllOrders].sort(byCreatedAtDesc));
      setPendingPayments([...loadedPendingPayments].sort(byCreatedAtDesc));
      setPendingSelections([...loadedPendingSelections].sort(byCreatedAtDesc));
    } catch {
      if (requestSeq.current !== requestId) return;
      setLoadFailed(true);
      setPendingOrders([]);
      setAllOrders([]);
      setPendingPayments([]);
      setPendingSelections([]);
    } finally {
      if (requestSeq.current === requestId) setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
    return () => {
      requestSeq.current += 1;
    };
  }, [loadDashboard]);

  if (loading) {
    return (
      <PageContainer title="工作台">
        <Skeleton active paragraph={{ rows: 10 }} />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="工作台">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        {loadFailed ? (
          <Alert
            showIcon
            type="warning"
            message="工作台数据加载失败，请稍后重试"
            action={
              <Button size="small" onClick={loadDashboard}>
                重试
              </Button>
            }
          />
        ) : null}

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} xl={6}>
            <ProCard>
              <Statistic title="待销售确认" value={pendingOrders.length} />
            </ProCard>
          </Col>
          <Col xs={24} sm={12} xl={6}>
            <ProCard>
              <Statistic title="今日新增订单" value={countTodayOrders(allOrders)} />
            </ProCard>
          </Col>
          <Col xs={24} sm={12} xl={6}>
            <ProCard>
              <Statistic title="待财务确认" value={pendingPayments.length} />
            </ProCard>
          </Col>
          <Col xs={24} sm={12} xl={6}>
            <ProCard>
              <Statistic title="待选位审核" value={pendingSelections.length} />
            </ProCard>
          </Col>
        </Row>

        <ProCard split="vertical">
          <ProCard
            title="销售确认待办"
            extra={
              <Button type="link" onClick={() => navigate('/admin/orders?status=PENDING_SALES_CONFIRM')}>
                查看全部
              </Button>
            }
          >
            <ProList<ParticipationOrder>
              rowKey="id"
              dataSource={pendingOrders.slice(0, 100)}
              locale={{ emptyText: '暂无待确认订单' }}
              metas={{
                title: { render: (_, record) => `订单 ${formatShortId(record.id)}` },
                description: {
                  render: (_, record) => (
                    <Space size={8} wrap>
                      <StatusTag meta={getOrderStatusMeta(record.status)} />
                      <Typography.Text type="secondary">{formatArea(record.areaSqm)}</Typography.Text>
                      <Typography.Text type="secondary">{formatMoney(record.totalAmount)}</Typography.Text>
                    </Space>
                  ),
                },
                actions: {
                  render: (_, record) => [
                    <Button key="detail" type="link" size="small" onClick={() => navigate(`/admin/orders/${record.id}`)}>
                      查看详情
                    </Button>,
                  ],
                },
              }}
            />
          </ProCard>

          <ProCard
            title="财务确认待办"
            extra={
              <Button type="link" onClick={() => navigate('/admin/payments')}>
                查看全部
              </Button>
            }
          >
            <ProList<PaymentRecord>
              rowKey="id"
              dataSource={pendingPayments.slice(0, 100)}
              locale={{ emptyText: '暂无待确认付款' }}
              metas={{
                title: { render: (_, record) => `付款 ${formatShortId(record.id)}` },
                description: {
                  render: (_, record) => (
                    <Space size={8} wrap>
                      <StatusTag meta={getPaymentStatusMeta(record.status)} />
                      <Typography.Text type="secondary">订单 {formatShortId(record.orderId)}</Typography.Text>
                      <Typography.Text type="secondary">{formatMoney(record.amount)}</Typography.Text>
                    </Space>
                  ),
                },
                actions: {
                  render: () => [
                    <Button key="payments" type="link" size="small" onClick={() => navigate('/admin/payments')}>
                      去处理
                    </Button>,
                  ],
                },
              }}
            />
          </ProCard>

          <ProCard
            title="选位审核待办"
            extra={
              <Button type="link" onClick={() => navigate('/admin/selections')}>
                查看全部
              </Button>
            }
          >
            <ProList<BoothSelection>
              rowKey="id"
              dataSource={pendingSelections.slice(0, 100)}
              locale={{ emptyText: '暂无待审核选位' }}
              metas={{
                title: { render: (_, record) => `选位 ${formatShortId(record.id)}` },
                description: {
                  render: (_, record) => (
                    <Space size={8} wrap>
                      <StatusTag meta={getSelectionStatusMeta(record.status)} />
                      <Typography.Text type="secondary">订单 {formatShortId(record.orderId)}</Typography.Text>
                      <Typography.Text type="secondary">{formatArea(record.totalAreaSqm)}</Typography.Text>
                    </Space>
                  ),
                },
                actions: {
                  render: () => [
                    <Button key="selections" type="link" size="small" onClick={() => navigate('/admin/selections')}>
                      去处理
                    </Button>,
                  ],
                },
              }}
            />
          </ProCard>
        </ProCard>
      </Space>
    </PageContainer>
  );
}
