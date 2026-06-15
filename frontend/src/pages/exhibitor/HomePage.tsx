import { Button, Card, Empty, List, Result, Skeleton, Space, Statistic, Steps, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';
import { formatBoothType, formatEventDisplay, formatRegistrationStatus } from '../../app/display';
import type { BoothSelection, ParticipationOrder, Registration, SelectionEligibility } from '../../app/domain';
import { formatArea } from '../../app/formatters';
import { loadSelectionEligibility } from '../../app/apiLoaders';
import { apiGet } from '../../app/request';
import { useEvents } from '../../app/useEvents';
import {
  buildHomeDashboardSummary,
  buildHomeFlowSteps,
  buildHomeTodoItems,
  formatMobileAmount,
  formatMobileTime,
} from './mobileViewModels';
import {
  getLatestRegistration,
  getPrimaryRegistrationForHome,
  getRegistrationHomeAction,
} from './registrationViewModels';
import {
  getExhibitorLoadErrorCopy,
  getResponsiveStepsDirection,
  shouldUseMobileExhibitorView,
} from './pageLogic';

function byCreatedAtDesc<T extends { createdAt?: string | null }>(left: T, right: T) {
  return (Date.parse(right.createdAt ?? '') || 0) - (Date.parse(left.createdAt ?? '') || 0);
}

export default function HomePage() {
  const navigate = useNavigate();
  const { events } = useEvents();
  const isMobile = shouldUseMobileExhibitorView();
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState<ParticipationOrder[]>([]);
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [latestSelectionsByOrderId, setLatestSelectionsByOrderId] = useState<Record<string, BoothSelection | undefined>>({});
  const [eligibilitiesByOrderId, setEligibilitiesByOrderId] = useState<Record<string, SelectionEligibility | undefined>>({});
  const [loadError, setLoadError] = useState(false);

  const stepsDirection = getResponsiveStepsDirection(isMobile);
  const homeOrders = useMemo(
    () => orders.map((order) => ({
      ...order,
      eventName: formatEventDisplay(order.eventId, events),
    })),
    [events, orders],
  );
  const dashboardSummary = useMemo(
    () => buildHomeDashboardSummary({ orders: homeOrders, eligibilitiesByOrderId, latestSelectionsByOrderId }),
    [eligibilitiesByOrderId, homeOrders, latestSelectionsByOrderId],
  );
  const todoItems = useMemo(
    () => buildHomeTodoItems({ orders: homeOrders, eligibilitiesByOrderId, latestSelectionsByOrderId }),
    [eligibilitiesByOrderId, homeOrders, latestSelectionsByOrderId],
  );
  const latestRegistration = useMemo(() => getLatestRegistration(registrations), [registrations]);
  const primaryRegistration = useMemo(() => getPrimaryRegistrationForHome(registrations), [registrations]);
  const registrationAction = useMemo(() => getRegistrationHomeAction(primaryRegistration), [primaryRegistration]);

  useEffect(() => {
    let alive = true;
    async function load() {
      setLoading(true);
      setLoadError(false);
      try {
        const [loadedOrders, loadedRegistrations] = await Promise.all([
          apiGet<ParticipationOrder[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`),
          apiGet<Registration[]>(`/api/registrations?exhibitorId=${DEMO_EXHIBITOR_ID}`),
        ]);
        if (!alive) return;
        setOrders(loadedOrders);
        setRegistrations(loadedRegistrations);
        if (!loadedOrders.length) {
          setLatestSelectionsByOrderId({});
          setEligibilitiesByOrderId({});
          return;
        }
        const relatedResults = await Promise.all(
          loadedOrders.map(async (order) => {
            try {
              const [loadedSelections, loadedEligibility] = await Promise.all([
                apiGet<BoothSelection[]>(`/api/booth-selections?orderId=${order.id}`),
                loadSelectionEligibility(order.id),
              ]);
              return {
                orderId: order.id,
                latestSelection: [...loadedSelections].sort(byCreatedAtDesc)[0],
                eligibility: loadedEligibility,
              };
            } catch {
              return { orderId: order.id, latestSelection: undefined, eligibility: undefined };
            }
          }),
        );
        if (!alive) return;
        setLatestSelectionsByOrderId(Object.fromEntries(relatedResults.map((item) => [item.orderId, item.latestSelection])));
        setEligibilitiesByOrderId(Object.fromEntries(relatedResults.map((item) => [item.orderId, item.eligibility])));
      } catch {
        if (!alive) return;
        setOrders([]);
        setRegistrations([]);
        setLatestSelectionsByOrderId({});
        setEligibilitiesByOrderId({});
        setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }
    load();
    return () => { alive = false; };
  }, []);

  if (loading) {
    return (
      <div className="exhibitor-page">
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    );
  }

  if (loadError) {
    const copy = getExhibitorLoadErrorCopy('home');
    return (
      <div className="exhibitor-page">
        <Card>
          <Result
            status="error"
            title={copy.title}
            subTitle={copy.subTitle}
            extra={<Button type="primary" onClick={() => window.location.reload()}>{copy.actionText}</Button>}
          />
        </Card>
      </div>
    );
  }

  if (!orders.length) {
    return (
      <div className="exhibitor-page">
        <div className="exhibitor-page-heading">
          <Typography.Title level={4}>我的参展概览</Typography.Title>
          <Typography.Text type="secondary">第一次参展也没关系，按流程一步步完成就行。</Typography.Text>
        </div>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Card title="参展流程">
            <Steps
              current={0}
              direction={stepsDirection}
              items={buildHomeFlowSteps()}
            />
          </Card>
          <Card title="报名状态">
            <Space direction="vertical" size={10} style={{ width: '100%' }}>
              <Typography.Text strong>
                {latestRegistration ? formatRegistrationStatus(latestRegistration.status) : '尚未提交报名'}
              </Typography.Text>
              <Typography.Text type="secondary">
                {latestRegistration
                  ? `${formatEventDisplay(latestRegistration.eventId, events)} · ${latestRegistration.categoryName || latestRegistration.category || '未选择类别'}`
                  : '先提交报名，审核通过后即可创建参展订单。'}
              </Typography.Text>
              <Button
                type="primary"
                block
                disabled={registrationAction.disabled}
                onClick={() => navigate(registrationAction.to)}
              >
                {registrationAction.label}
              </Button>
            </Space>
          </Card>
          <Card>
            <Empty
              description={
                <Space direction="vertical" size={4}>
                  <Typography.Text>您还没有参展订单</Typography.Text>
                  <Typography.Text type="secondary">报名审核通过后，可以创建订单并在这里查看进度。</Typography.Text>
                </Space>
              }
            >
              <Button type="primary" onClick={() => navigate(registrationAction.to)}>{registrationAction.label}</Button>
            </Empty>
          </Card>
        </Space>
      </div>
    );
  }

  return (
    <div className="exhibitor-page">
      <div className="exhibitor-page-heading">
        <Typography.Title level={4}>我的参展概览</Typography.Title>
        <Typography.Text type="secondary">所有订单的状态和待办都在这里汇总。</Typography.Text>
      </div>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Card title="进行中的订单">
          <div className="exhibitor-home-stats">
            <Statistic title="待付款" value={dashboardSummary.pendingPaymentCount} suffix="笔" />
            <Statistic title="待选位" value={dashboardSummary.readyForSelectionCount} suffix="笔" />
            <Statistic title="审核中" value={dashboardSummary.selectionReviewCount} suffix="笔" />
            <Statistic title="已完成" value={dashboardSummary.completedCount} suffix="笔" />
          </div>
          <Typography.Text type="secondary">
            共 {dashboardSummary.totalCount} 笔订单，其他进行中 {dashboardSummary.otherInProgressCount} 笔
          </Typography.Text>
        </Card>

        <Card title="报名状态">
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            <Typography.Text strong>
              {latestRegistration ? formatRegistrationStatus(latestRegistration.status) : '尚未提交报名'}
            </Typography.Text>
            <Typography.Text type="secondary">
              {latestRegistration
                ? `${formatEventDisplay(latestRegistration.eventId, events)} · ${latestRegistration.categoryName || latestRegistration.category || '未选择类别'}`
                : '先提交报名，审核通过后即可创建参展订单。'}
            </Typography.Text>
            <Button
              type="primary"
              block
              disabled={registrationAction.disabled}
              onClick={() => navigate(registrationAction.to)}
            >
              {registrationAction.label}
            </Button>
          </Space>
        </Card>

        <Card
          title="待办事项"
          extra={<Button type="link" onClick={() => navigate('/exhibitor/orders')}>全部订单</Button>}
        >
          {todoItems.length ? (
            <List
              dataSource={todoItems}
              renderItem={(todo) => (
                <List.Item style={{ paddingInline: 0 }}>
                  <Card
                    className="exhibitor-task-card"
                    onClick={() => navigate(todo.to)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        navigate(todo.to);
                      }
                    }}
                  >
                    <Space direction="vertical" size={10} style={{ width: '100%' }}>
                      <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                        <Typography.Text strong>{todo.title}</Typography.Text>
                        <Typography.Text type="secondary">更新 {formatMobileTime(orders.find((order) => order.id === todo.orderId)?.updatedAt)}</Typography.Text>
                      </Space>
                      <Typography.Text type="secondary">{todo.description}</Typography.Text>
                      <Button type="primary" block disabled={todo.disabled} onClick={(event) => {
                        event.stopPropagation();
                        navigate(todo.to);
                      }}>
                        {todo.actionLabel}
                      </Button>
                    </Space>
                  </Card>
                </List.Item>
              )}
            />
          ) : (
            <Empty description="暂无待处理事项">
              <Button onClick={() => navigate('/exhibitor/orders')}>查看全部订单</Button>
            </Empty>
          )}
        </Card>

        <Card title="参展流程">
          <Steps
            current={0}
            direction={stepsDirection}
            items={buildHomeFlowSteps()}
          />
        </Card>

        <Card title="最近订单">
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            {[...orders].sort(byCreatedAtDesc).slice(0, 3).map((order) => (
              <Card
                key={order.id}
                className="exhibitor-order-summary-card"
                onClick={() => navigate(`/exhibitor/orders/${order.id}`)}
                role="button"
                tabIndex={0}
              >
                <Space direction="vertical" size={6} style={{ width: '100%' }}>
                  <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                    <Typography.Text strong>{formatEventDisplay(order.eventId, events)}</Typography.Text>
                    <Typography.Text className="exhibitor-order-amount">{formatMobileAmount(order.totalAmount)}</Typography.Text>
                  </Space>
                  <Typography.Text type="secondary">
                    {formatArea(order.areaSqm)} · {formatBoothType(order.boothType)} · 更新 {formatMobileTime(order.updatedAt)}
                  </Typography.Text>
                </Space>
              </Card>
            ))}
          </Space>
        </Card>
      </Space>
    </div>
  );
}
