import { PageContainer, ProCard, ProList } from '@ant-design/pro-components';
import { Alert, Button, Col, Row, Skeleton, Space, Statistic, Tag, Typography } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ADMIN_DASHBOARD_REFRESH_EVENT, loadAdminDashboard } from '../../app/apiLoaders';
import { formatEventDisplay, formatExhibitorDisplay, formatRegistrationStatus } from '../../app/display';
import type { AdminDashboardResponse, BoothSelection, EventCategory, Exhibitor, ParticipationOrder, PaymentRecord, Registration } from '../../app/domain';
import { formatArea, formatFriendlyTime, formatMoney, formatShortId } from '../../app/formatters';
import { apiGet } from '../../app/request';
import { getOrderStatusMeta, getPaymentStatusMeta, getSelectionStatusMeta, StatusTag } from '../../app/status';
import { useEvents } from '../../app/useEvents';
import { buildDashboardViewModel } from './dashboard/dashboardLogic';

function getRegistrationStatusColor(status: Registration['status']) {
  if (status === 'APPROVED') return 'green';
  if (status === 'REJECTED') return 'red';
  if (status === 'SUBMITTED') return 'blue';
  if (status === 'CANCELLED') return 'default';
  return 'gold';
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const requestSeq = useRef(0);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [dashboard, setDashboard] = useState<AdminDashboardResponse>();
  const [exhibitors, setExhibitors] = useState<Exhibitor[]>([]);
  const [categories, setCategories] = useState<EventCategory[]>([]);
  const { events } = useEvents();

  const loadDashboard = useCallback(async () => {
    const requestId = requestSeq.current + 1;
    requestSeq.current = requestId;
    setLoading(true);
    setLoadFailed(false);

    try {
      const [loadedDashboard, loadedExhibitors] = await Promise.all([
        loadAdminDashboard(),
        apiGet<Exhibitor[]>('/api/exhibitors').catch(() => []),
      ]);
      if (requestSeq.current !== requestId) return;
      setDashboard(loadedDashboard);
      setExhibitors(loadedExhibitors);
      const eventIds = [...new Set((loadedDashboard.pendingRegistrationReviews ?? [])
        .map((registration) => registration.eventId)
        .filter(Boolean))];
      const loadedCategories = await Promise.all(
        eventIds.map((eventId) => apiGet<EventCategory[]>(
          `/api/event-categories?eventId=${encodeURIComponent(eventId)}&enabled=true`,
        ).catch(() => [])),
      );
      if (requestSeq.current !== requestId) return;
      setCategories(loadedCategories.flat());
    } catch {
      if (requestSeq.current !== requestId) return;
      setLoadFailed(true);
      setDashboard(undefined);
    } finally {
      if (requestSeq.current === requestId) setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard, location.key]);

  useEffect(() => {
    window.addEventListener('focus', loadDashboard);
    window.addEventListener(ADMIN_DASHBOARD_REFRESH_EVENT, loadDashboard);
    return () => {
      window.removeEventListener('focus', loadDashboard);
      window.removeEventListener(ADMIN_DASHBOARD_REFRESH_EVENT, loadDashboard);
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

  const viewModel = buildDashboardViewModel(dashboard ?? {});
  const pendingRegistrations = viewModel.pendingRegistrationReviews.slice(0, 5);
  const pendingOrders = viewModel.pendingSalesConfirmOrders.slice(0, 5);
  const pendingPayments = viewModel.pendingFinanceConfirmPayments.slice(0, 5);
  const pendingSelections = viewModel.pendingSelectionReviews.slice(0, 5);
  const exhibitorNameById = new Map(exhibitors.map((exhibitor) => [exhibitor.id, exhibitor.companyName]));
  const categoryNameByEventAndCode = new Map(
    categories.map((category) => [`${category.eventId}:${category.code}`, category.name]),
  );

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
          {viewModel.kpis.map((kpi) => (
            <Col key={kpi.title} xs={24} sm={12} xl={8} xxl={4}>
              <ProCard
                hoverable={Boolean(kpi.to)}
                onClick={kpi.to ? () => {
                  if (kpi.to) navigate(kpi.to);
                } : undefined}
              >
                <Statistic title={kpi.title} value={kpi.value} />
              </ProCard>
            </Col>
          ))}
        </Row>

        <Typography.Text type="secondary">生成时间：{viewModel.generatedAtDisplay}</Typography.Text>

        <ProCard split="vertical">
          <ProCard
            title="报名审核待办"
            extra={
              <Button type="link" onClick={() => navigate('/admin/registrations?status=SUBMITTED')}>
                查看全部
              </Button>
            }
          >
            <ProList<Registration>
              rowKey="id"
              dataSource={pendingRegistrations}
              locale={{ emptyText: '暂无待审核报名' }}
              metas={{
                title: { render: (_, record) => formatEventDisplay(record.eventId, events) },
                description: {
                  render: (_, record) => (
                    <Space size={8} wrap>
                      <Typography.Text type="secondary">
                        {exhibitorNameById.get(record.exhibitorId) || formatExhibitorDisplay(record.exhibitorId)}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {record.categoryName
                          || (record.category ? categoryNameByEventAndCode.get(`${record.eventId}:${record.category}`) : undefined)
                          || record.category
                          || '-'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {record.submittedAt ? formatShortId(record.id) : '未提交'}
                      </Typography.Text>
                    </Space>
                  ),
                },
                subTitle: {
                  render: (_, record) => (
                    <Typography.Text type="secondary">
                      {formatFriendlyTime(record.submittedAt || record.updatedAt)}
                    </Typography.Text>
                  ),
                },
                actions: {
                  render: (_, record) => [
                    <Button
                      key="detail"
                      type="link"
                      size="small"
                      onClick={() => navigate(`/admin/registrations/${record.id}`)}
                    >
                      审核
                    </Button>,
                  ],
                },
                avatar: {
                  render: (_, record) => (
                    <Tag color={getRegistrationStatusColor(record.status)}>
                      {formatRegistrationStatus(record.status)}
                    </Tag>
                  ),
                },
              }}
            />
          </ProCard>

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
              dataSource={pendingOrders}
              locale={{ emptyText: '暂无待处理事项' }}
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
              dataSource={pendingPayments}
              locale={{ emptyText: '暂无待处理事项' }}
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
              dataSource={pendingSelections}
              locale={{ emptyText: '暂无待处理事项' }}
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
