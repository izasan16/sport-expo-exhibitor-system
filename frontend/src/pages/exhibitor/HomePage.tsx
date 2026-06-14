import { PageContainer, ProCard, ProList } from '@ant-design/pro-components';
import { Button, Empty, Result, Skeleton, Space, Statistic, Steps, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';
import { formatEventDisplay } from '../../app/display';
import type { BoothSelection, PaymentRecord, ParticipationOrder, SelectionEligibility } from '../../app/domain';
import { formatArea, formatShortId } from '../../app/formatters';
import { loadSelectionEligibility } from '../../app/apiLoaders';
import { apiGet } from '../../app/request';
import { getOrderStatusMeta, getPaymentStatusMeta, getSelectionStatusMeta, StatusTag } from '../../app/status';
import { useEvents } from '../../app/useEvents';
import { buildHomeFlowSteps, formatMobileAmount, formatMobileText, formatMobileTime } from './mobileViewModels';
import { getExhibitorLoadErrorCopy, getOrderNextAction, getOrderStepIndex, pickLatestActiveOrder } from './pageLogic';

function byCreatedAtDesc<T extends { createdAt?: string | null }>(left: T, right: T) {
  return (Date.parse(right.createdAt ?? '') || 0) - (Date.parse(left.createdAt ?? '') || 0);
}

export default function HomePage() {
  const navigate = useNavigate();
  const { events } = useEvents();
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState<ParticipationOrder[]>([]);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [selections, setSelections] = useState<BoothSelection[]>([]);
  const [eligibility, setEligibility] = useState<SelectionEligibility>();
  const [loadError, setLoadError] = useState(false);

  const activeOrder = pickLatestActiveOrder(orders);
  const latestSelection = selections[0];
  const orderStatusMeta = activeOrder?.status ? getOrderStatusMeta(activeOrder.status) : { label: '未填写', color: 'default' };
  const activeOrderEvent = activeOrder?.eventId ? formatEventDisplay(activeOrder.eventId, events) : '未填写';
  const nextAction = getOrderNextAction({
    order: activeOrder,
    eligibilityStatus: eligibility?.status,
    latestSelectionStatus: latestSelection?.status,
  });

  useEffect(() => {
    let alive = true;
    async function load() {
      setLoading(true);
      setLoadError(false);
      try {
        const loadedOrders = await apiGet<ParticipationOrder[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`);
        if (!alive) return;
        setOrders(loadedOrders);
        const selectedOrder = pickLatestActiveOrder(loadedOrders);
        if (!selectedOrder) {
          setPayments([]);
          setSelections([]);
          setEligibility(undefined);
          return;
        }
        const [loadedPayments, loadedSelections, loadedEligibility] = await Promise.all([
          apiGet<PaymentRecord[]>(`/api/payments?orderId=${selectedOrder.id}`),
          apiGet<BoothSelection[]>(`/api/booth-selections?orderId=${selectedOrder.id}`),
          loadSelectionEligibility(selectedOrder.id),
        ]);
        if (!alive) return;
        setPayments([...loadedPayments].sort(byCreatedAtDesc));
        setSelections([...loadedSelections].sort(byCreatedAtDesc));
        setEligibility(loadedEligibility);
      } catch {
        if (!alive) return;
        setOrders([]);
        setPayments([]);
        setSelections([]);
        setEligibility(undefined);
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
      <PageContainer title="首页">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (loadError) {
    const copy = getExhibitorLoadErrorCopy('home');
    return (
      <PageContainer title="首页">
        <ProCard>
          <Result
            status="error"
            title={copy.title}
            subTitle={copy.subTitle}
            extra={<Button type="primary" onClick={() => window.location.reload()}>{copy.actionText}</Button>}
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (!activeOrder) {
    return (
      <PageContainer title="首页">
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <ProCard title="参展流程">
            <Steps
              current={0}
              items={buildHomeFlowSteps()}
            />
          </ProCard>
          <ProCard>
            <Empty description="您还没有参展订单">
              <Button type="primary" onClick={() => navigate('/exhibitor/orders/new')}>我要参展</Button>
            </Empty>
          </ProCard>
        </Space>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="首页">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard split="vertical">
          <ProCard title={`订单 ${formatShortId(activeOrder.id)}`}>
            <Space direction="vertical" size={12}>
              <StatusTag meta={orderStatusMeta} />
              <Typography.Text>展会 {formatMobileText(activeOrderEvent)}</Typography.Text>
              <Typography.Text>面积 {formatArea(activeOrder.areaSqm)} · {formatMobileText(activeOrder.boothType)}</Typography.Text>
              <Button type="primary" disabled={nextAction.disabled} onClick={() => navigate(nextAction.to)}>
                {nextAction.label}
              </Button>
            </Space>
          </ProCard>
          <ProCard>
            <Statistic title="订单金额" value={formatMobileAmount(activeOrder.totalAmount)} />
          </ProCard>
        </ProCard>
        <ProCard title="参展进度">
          <Steps
            current={getOrderStepIndex(activeOrder.status)}
            items={buildHomeFlowSteps()}
          />
        </ProCard>
        <ProCard split="vertical">
          <ProCard title="最近付款">
            <ProList<PaymentRecord>
              rowKey="id"
              dataSource={payments.slice(0, 3)}
              locale={{ emptyText: '暂无付款记录' }}
              metas={{
                title: { render: (_, record) => formatMobileAmount(record.amount) },
                description: {
                  render: (_, record) => (
                    <Space>
                      <StatusTag meta={record.status ? getPaymentStatusMeta(record.status) : { label: '未填写', color: 'default' }} />
                      <Typography.Text type="secondary">{formatMobileTime(record.createdAt)}</Typography.Text>
                    </Space>
                  ),
                },
              }}
            />
          </ProCard>
          <ProCard title="最近选位">
            <ProList<BoothSelection>
              rowKey="id"
              dataSource={selections.slice(0, 3)}
              locale={{ emptyText: '暂无选位记录' }}
              metas={{
                title: { render: (_, record) => `选位 ${formatShortId(record.id)}` },
                description: {
                  render: (_, record) => (
                    <Space>
                      <StatusTag meta={record.status ? getSelectionStatusMeta(record.status) : { label: '未填写', color: 'default' }} />
                      <Typography.Text type="secondary">{formatMobileTime(record.createdAt)}</Typography.Text>
                    </Space>
                  ),
                },
              }}
            />
          </ProCard>
        </ProCard>
      </Space>
    </PageContainer>
  );
}
