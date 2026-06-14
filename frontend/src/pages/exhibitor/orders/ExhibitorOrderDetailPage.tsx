import { CopyOutlined } from '@ant-design/icons';
import { PageContainer, ProCard, ProDescriptions, ProTable } from '@ant-design/pro-components';
import type { ProColumns, ProDescriptionsItemProps } from '@ant-design/pro-components';
import { Button, Empty, message, Result, Skeleton, Space, Statistic, Steps, Tabs, Tooltip, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { loadSelectionEligibility } from '../../../app/apiLoaders';
import { formatAreaTierDisplay, formatEventDisplay, formatExhibitorDisplay, type AreaTierLookupSource } from '../../../app/display';
import type { BoothSelection, ContractRecord, ParticipationOrder, PaymentRecord, SelectionEligibility } from '../../../app/domain';
import { formatArea, formatFriendlyTime, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet } from '../../../app/request';
import {
  getContractStatusMeta,
  getEligibilityStatusMeta,
  getOrderStatusMeta,
  getPaymentStatusMeta,
  getSelectionStatusMeta,
  StatusTag,
} from '../../../app/status';
import { useEvents } from '../../../app/useEvents';
import { getExhibitorLoadErrorCopy, getOrderNextAction, getOrderStepIndex, isOrderPayable } from '../pageLogic';

function byCreatedAtDesc<T extends { createdAt: string }>(left: T, right: T) {
  return Date.parse(right.createdAt) - Date.parse(left.createdAt);
}

export default function ExhibitorOrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<ParticipationOrder>();
  const [contracts, setContracts] = useState<ContractRecord[]>([]);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [selections, setSelections] = useState<BoothSelection[]>([]);
  const [eligibility, setEligibility] = useState<SelectionEligibility>();
  const [areaTiers, setAreaTiers] = useState<AreaTierLookupSource[]>([]);
  const [loadError, setLoadError] = useState(false);
  const { events } = useEvents();

  useEffect(() => {
    let alive = true;

    async function load() {
      if (!orderId) {
        setOrder(undefined);
        setContracts([]);
        setPayments([]);
        setSelections([]);
        setEligibility(undefined);
        setLoading(false);
        return;
      }

      setLoading(true);
      setLoadError(false);
      try {
        const [loadedOrder, loadedContracts, loadedPayments, loadedSelections, loadedEligibility] = await Promise.all([
          apiGet<ParticipationOrder>(`/api/participation-orders/${orderId}`),
          apiGet<ContractRecord[]>(`/api/contracts?orderId=${orderId}`),
          apiGet<PaymentRecord[]>(`/api/payments?orderId=${orderId}`),
          apiGet<BoothSelection[]>(`/api/booth-selections?orderId=${orderId}`),
          loadSelectionEligibility(orderId),
        ]);
        const loadedAreaTiers = await apiGet<AreaTierLookupSource[]>(`/api/area-tiers?eventId=${encodeURIComponent(loadedOrder.eventId)}`);

        if (!alive) return;
        setOrder(loadedOrder);
        setContracts([...loadedContracts].sort(byCreatedAtDesc));
        setPayments([...loadedPayments].sort(byCreatedAtDesc));
        setSelections([...loadedSelections].sort(byCreatedAtDesc));
        setEligibility(loadedEligibility);
        setAreaTiers(loadedAreaTiers);
      } catch {
        if (!alive) return;
        setOrder(undefined);
        setContracts([]);
        setPayments([]);
        setSelections([]);
        setEligibility(undefined);
        setAreaTiers([]);
        setLoadError(true);
      } finally {
        if (alive) setLoading(false);
      }
    }

    load();
    return () => {
      alive = false;
    };
  }, [orderId]);

  const latestSelection = selections[0];
  const nextAction = getOrderNextAction({
    order,
    eligibilityStatus: eligibility?.status,
    latestSelectionStatus: latestSelection?.status,
  });
  const canChooseBooth = order?.status === 'PAID' && eligibility?.status === 'AVAILABLE';

  const basicColumns = useMemo<ProDescriptionsItemProps<ParticipationOrder>[]>(
    () => [
      { title: '订单 ID', dataIndex: 'id', copyable: true, ellipsis: true },
      { title: '展会', dataIndex: 'eventId', render: (_, record) => formatEventDisplay(record.eventId, events) },
      { title: '展商', dataIndex: 'exhibitorId', render: (_, record) => formatExhibitorDisplay(record.exhibitorId) },
      { title: '展区/分区', dataIndex: 'zoneId', renderText: (value) => value || '不限分区' },
      { title: '面积档位', dataIndex: 'areaTierId', render: (_, record) => formatAreaTierDisplay(record.areaTierId, areaTiers) },
      { title: '展位类型', dataIndex: 'boothType' },
      { title: '面积', dataIndex: 'areaSqm', render: (_, record) => formatArea(record.areaSqm) },
      { title: '订单状态', dataIndex: 'status', render: (_, record) => <StatusTag meta={getOrderStatusMeta(record.status)} /> },
      { title: '面积费', dataIndex: 'areaFee', render: (_, record) => formatMoney(record.areaFee) },
      { title: '服务费', dataIndex: 'serviceFee', render: (_, record) => formatMoney(record.serviceFee) },
      { title: '优惠金额', dataIndex: 'discountAmount', render: (_, record) => formatMoney(record.discountAmount) },
      { title: '调整金额', dataIndex: 'adjustmentAmount', render: (_, record) => formatMoney(record.adjustmentAmount) },
      { title: '总金额', dataIndex: 'totalAmount', render: (_, record) => formatMoney(record.totalAmount) },
      { title: '创建时间', dataIndex: 'createdAt', render: (_, record) => formatFriendlyTime(record.createdAt) },
      { title: '更新时间', dataIndex: 'updatedAt', render: (_, record) => formatFriendlyTime(record.updatedAt) },
    ],
    [areaTiers, events],
  );

  const paymentColumns: ProColumns<PaymentRecord>[] = [
    {
      title: '付款',
      dataIndex: 'id',
      ellipsis: true,
      render: (_, record) => `付款 ${formatShortId(record.id)}`,
    },
    {
      title: '金额',
      dataIndex: 'amount',
      render: (_, record) => formatMoney(record.amount),
    },
    { title: '方式', dataIndex: 'method' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getPaymentStatusMeta(record.status)} />,
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      render: (_, record) => formatFriendlyTime(record.createdAt),
    },
    {
      title: '确认时间',
      dataIndex: 'confirmedAt',
      render: (_, record) => formatFriendlyTime(record.confirmedAt),
    },
  ];

  const selectionColumns: ProColumns<BoothSelection>[] = [
    {
      title: '选位',
      dataIndex: 'id',
      ellipsis: true,
      render: (_, record) => `选位 ${formatShortId(record.id)}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getSelectionStatusMeta(record.status)} />,
    },
    {
      title: '面积',
      dataIndex: 'totalAreaSqm',
      render: (_, record) => formatArea(record.totalAreaSqm),
    },
    {
      title: '展位数',
      dataIndex: 'boothIds',
      render: (_, record) => `${record.boothIds.length} 个`,
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      render: (_, record) => formatFriendlyTime(record.createdAt),
    },
    {
      title: '审核时间',
      dataIndex: 'reviewedAt',
      render: (_, record) => formatFriendlyTime(record.reviewedAt),
    },
  ];

  if (loading) {
    return (
      <PageContainer title="订单详情">
        <Skeleton active paragraph={{ rows: 10 }} />
      </PageContainer>
    );
  }

  if (loadError) {
    const copy = getExhibitorLoadErrorCopy('detail');
    return (
      <PageContainer title="订单详情">
        <ProCard>
          <Result
            status="error"
            title={copy.title}
            subTitle={copy.subTitle}
            extra={
              <Space>
                <Button onClick={() => navigate('/exhibitor/orders')}>返回我的订单</Button>
                <Button type="primary" onClick={() => window.location.reload()}>{copy.actionText}</Button>
              </Space>
            }
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (!order) {
    return (
      <PageContainer title="订单详情">
        <ProCard>
          <Empty description="未找到订单">
            <Button type="primary" onClick={() => navigate('/exhibitor/orders')}>
              返回我的订单
            </Button>
          </Empty>
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title={`订单 ${formatShortId(order.id)}`}
      extra={[
        <Tooltip key="copy" title="复制完整订单 ID">
          <Button
            icon={<CopyOutlined />}
            onClick={async () => {
              await navigator.clipboard.writeText(order.id);
              message.success('订单 ID 已复制');
            }}
          />
        </Tooltip>,
        <Tooltip key="next" title={nextAction.reason}>
          <Button type="primary" disabled={nextAction.disabled} onClick={() => navigate(nextAction.to)}>
            {nextAction.label}
          </Button>
        </Tooltip>,
      ]}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard split="vertical">
          <ProCard>
            <Space direction="vertical" size={8}>
              <Typography.Text type="secondary">订单状态</Typography.Text>
              <StatusTag meta={getOrderStatusMeta(order.status)} />
            </Space>
          </ProCard>
          <ProCard>
            <Statistic title="面积" value={formatArea(order.areaSqm)} />
          </ProCard>
          <ProCard>
            <Statistic title="展位类型" value={order.boothType} />
          </ProCard>
          <ProCard>
            <Statistic title="订单金额" value={formatMoney(order.totalAmount)} />
          </ProCard>
        </ProCard>

        <ProCard title="参展进度">
          <Steps
            current={getOrderStepIndex(order.status)}
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

        <ProCard>
          <Tabs
            items={[
              {
                key: 'basic',
                label: '基本信息',
                children: <ProDescriptions<ParticipationOrder> column={2} dataSource={order} columns={basicColumns} />,
              },
              {
                key: 'contracts',
                label: '合同',
                children: contracts.length ? (
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    {contracts.map((contract) => (
                      <ProCard
                        key={contract.id}
                        title={contract.contractNo}
                        extra={<StatusTag meta={getContractStatusMeta(contract.status)} />}
                      >
                        <Space direction="vertical" size={8}>
                          <Typography.Text type="secondary">合同 ID：{contract.id}</Typography.Text>
                          <Typography.Text>当前版本：{contract.currentVersionId ?? '-'}</Typography.Text>
                          <Typography.Text>创建时间：{formatFriendlyTime(contract.createdAt)}</Typography.Text>
                          <Typography.Text>更新时间：{formatFriendlyTime(contract.updatedAt)}</Typography.Text>
                        </Space>
                      </ProCard>
                    ))}
                  </Space>
                ) : (
                  <Empty description="暂无合同" />
                ),
              },
              {
                key: 'payments',
                label: '付款',
                children: (
                  <ProTable<PaymentRecord>
                    rowKey="id"
                    search={false}
                    options={false}
                    pagination={false}
                    columns={paymentColumns}
                    dataSource={payments}
                    locale={{ emptyText: '暂无付款记录' }}
                    toolBarRender={() =>
                      isOrderPayable(order.status)
                        ? [
                            <Button key="submit-payment" type="primary" onClick={() => navigate(`/exhibitor/orders/${order.id}/payment`)}>
                              提交付款
                            </Button>,
                          ]
                        : []
                    }
                  />
                ),
              },
              {
                key: 'selections',
                label: '选位',
                children: (
                  <ProTable<BoothSelection>
                    rowKey="id"
                    search={false}
                    options={false}
                    pagination={false}
                    columns={selectionColumns}
                    dataSource={selections}
                    locale={{ emptyText: '暂无选位记录' }}
                    toolBarRender={() => [
                      <Space key="selection-actions">
                        {eligibility ? <StatusTag meta={getEligibilityStatusMeta(eligibility.status)} /> : null}
                        {canChooseBooth ? (
                          <Button type="primary" onClick={() => navigate(`/exhibitor/orders/${order.id}/selection`)}>
                            选择展位
                          </Button>
                        ) : null}
                      </Space>,
                    ]}
                  />
                ),
              },
            ]}
          />
        </ProCard>
      </Space>
    </PageContainer>
  );
}
