import { PageContainer, ProCard, ProList } from '@ant-design/pro-components';
import { Button, Empty, Result, Skeleton, Space, Statistic, Steps, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../app/demoActors';
import type { BoothSelection, PaymentRecord, ParticipationOrder, SelectionEligibility } from '../../app/domain';
import { formatArea, formatMoney, formatShortId } from '../../app/formatters';
import { loadSelectionEligibility } from '../../app/apiLoaders';
import { apiGet } from '../../app/request';
import { getOrderStatusMeta, getPaymentStatusMeta, getSelectionStatusMeta, StatusTag } from '../../app/status';
import { getExhibitorLoadErrorCopy, getOrderNextAction, getOrderStepIndex, pickLatestActiveOrder } from './pageLogic';

function byCreatedAtDesc<T extends { createdAt: string }>(left: T, right: T) {
  return Date.parse(right.createdAt) - Date.parse(left.createdAt);
}

export default function HomePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState<ParticipationOrder[]>([]);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [selections, setSelections] = useState<BoothSelection[]>([]);
  const [eligibility, setEligibility] = useState<SelectionEligibility>();
  const [loadError, setLoadError] = useState(false);

  const activeOrder = pickLatestActiveOrder(orders);
  const latestSelection = selections[0];
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
        <ProCard>
          <Empty description="您还没有参展订单">
            <Button type="primary" onClick={() => navigate('/exhibitor/orders/new')}>我要参展</Button>
          </Empty>
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="首页">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard split="vertical">
          <ProCard title={`订单 ${formatShortId(activeOrder.id)}`}>
            <Space direction="vertical" size={12}>
              <StatusTag meta={getOrderStatusMeta(activeOrder.status)} />
              <Typography.Text>面积 {formatArea(activeOrder.areaSqm)} · {activeOrder.boothType}</Typography.Text>
              <Button type="primary" disabled={nextAction.disabled} onClick={() => navigate(nextAction.to)}>
                {nextAction.label}
              </Button>
            </Space>
          </ProCard>
          <ProCard>
            <Statistic title="订单金额" value={formatMoney(activeOrder.totalAmount)} />
          </ProCard>
        </ProCard>
        <ProCard title="参展进度">
          <Steps
            current={getOrderStepIndex(activeOrder.status)}
            items={[
              { title: '下单' },
              { title: '销售确认' },
              { title: '合同' },
              { title: '付款' },
              { title: '选位' },
              { title: '完成' },
            ]}
          />
        </ProCard>
        <ProCard split="vertical">
          <ProCard title="最近付款">
            <ProList<PaymentRecord>
              rowKey="id"
              dataSource={payments.slice(0, 3)}
              locale={{ emptyText: '暂无付款记录' }}
              metas={{
                title: { render: (_, record) => formatMoney(record.amount) },
                description: { render: (_, record) => <StatusTag meta={getPaymentStatusMeta(record.status)} /> },
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
                description: { render: (_, record) => <StatusTag meta={getSelectionStatusMeta(record.status)} /> },
              }}
            />
          </ProCard>
        </ProCard>
      </Space>
    </PageContainer>
  );
}
