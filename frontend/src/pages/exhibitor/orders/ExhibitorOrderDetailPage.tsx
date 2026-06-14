import { CopyOutlined } from '@ant-design/icons';
import { PageContainer, ProCard, ProDescriptions, ProTable } from '@ant-design/pro-components';
import type { ProColumns, ProDescriptionsItemProps } from '@ant-design/pro-components';
import { Button, Collapse, Empty, List, message, Result, Skeleton, Space, Statistic, Steps, Tabs, Timeline, Tooltip, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { loadSelectionEligibility } from '../../../app/apiLoaders';
import { formatAreaTierDisplay, formatEventDisplay, formatExhibitorDisplay, formatPaymentMethod, type AreaTierLookupSource } from '../../../app/display';
import type { BoothSelection, ContractRecord, ParticipationOrder, PaymentRecord, SelectionEligibility } from '../../../app/domain';
import { formatArea, formatShortId } from '../../../app/formatters';
import { apiGet } from '../../../app/request';
import { useIsMobile } from '../../../app/responsive';
import {
  getContractStatusMeta,
  getEligibilityStatusMeta,
  getOrderStatusMeta,
  getPaymentStatusMeta,
  getSelectionStatusMeta,
  StatusTag,
} from '../../../app/status';
import { useEvents } from '../../../app/useEvents';
import { buildHomeFlowSteps, buildMobileTimelineEvents, formatMobileAmount, formatMobileOptional, formatMobileText, formatMobileTime } from '../mobileViewModels';
import { getExhibitorLoadErrorCopy, getOrderNextAction, getOrderStepIndex, isOrderPayable } from '../pageLogic';

function byCreatedAtDesc<T extends { createdAt?: string | null }>(left: T, right: T) {
  return (Date.parse(right.createdAt ?? '') || 0) - (Date.parse(left.createdAt ?? '') || 0);
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
  const isMobile = useIsMobile();

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
        const loadedAreaTiers = loadedOrder.eventId
          ? await apiGet<AreaTierLookupSource[]>(`/api/area-tiers?eventId=${encodeURIComponent(loadedOrder.eventId)}`)
          : [];

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
  const timelineEvents = useMemo(
    () => buildMobileTimelineEvents({ order, payments, selections, contracts }),
    [contracts, order, payments, selections],
  );

  const basicColumns = useMemo<ProDescriptionsItemProps<ParticipationOrder>[]>(
    () => [
      { title: '订单 ID', dataIndex: 'id', copyable: true, ellipsis: true },
      { title: '展会', dataIndex: 'eventId', render: (_, record) => (record.eventId ? formatEventDisplay(record.eventId, events) : '未填写') },
      { title: '展商', dataIndex: 'exhibitorId', render: (_, record) => (record.exhibitorId ? formatExhibitorDisplay(record.exhibitorId) : '未填写') },
      { title: '展区/分区', dataIndex: 'zoneId', renderText: (value) => value || '不限分区' },
      { title: '面积档位', dataIndex: 'areaTierId', render: (_, record) => (record.areaTierId ? formatAreaTierDisplay(record.areaTierId, areaTiers) : '未填写') },
      { title: '展位类型', dataIndex: 'boothType', render: (_, record) => formatMobileText(record.boothType) },
      { title: '面积', dataIndex: 'areaSqm', render: (_, record) => formatArea(record.areaSqm) },
      { title: '订单状态', dataIndex: 'status', render: (_, record) => <StatusTag meta={record.status ? getOrderStatusMeta(record.status) : { label: '未填写', color: 'default' }} /> },
      { title: '面积费', dataIndex: 'areaFee', render: (_, record) => formatMobileAmount(record.areaFee) },
      { title: '服务费', dataIndex: 'serviceFee', render: (_, record) => formatMobileAmount(record.serviceFee) },
      { title: '优惠金额', dataIndex: 'discountAmount', render: (_, record) => formatMobileAmount(record.discountAmount) },
      { title: '调整金额', dataIndex: 'adjustmentAmount', render: (_, record) => formatMobileAmount(record.adjustmentAmount) },
      { title: '总金额', dataIndex: 'totalAmount', render: (_, record) => formatMobileAmount(record.totalAmount) },
      { title: '创建时间', dataIndex: 'createdAt', render: (_, record) => formatMobileTime(record.createdAt) },
      { title: '更新时间', dataIndex: 'updatedAt', render: (_, record) => formatMobileTime(record.updatedAt) },
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
      render: (_, record) => formatMobileAmount(record.amount),
    },
    { title: '方式', dataIndex: 'method', render: (_, record) => formatPaymentMethod(record.method) },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={record.status ? getPaymentStatusMeta(record.status) : { label: '未填写', color: 'default' }} />,
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      render: (_, record) => formatMobileTime(record.createdAt),
    },
    {
      title: '确认时间',
      dataIndex: 'confirmedAt',
      render: (_, record) => formatMobileTime(record.confirmedAt),
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
      render: (_, record) => <StatusTag meta={record.status ? getSelectionStatusMeta(record.status) : { label: '未填写', color: 'default' }} />,
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
      render: (_, record) => formatMobileTime(record.createdAt),
    },
    {
      title: '审核时间',
      dataIndex: 'reviewedAt',
      render: (_, record) => formatMobileTime(record.reviewedAt),
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

  const orderStatusMeta = order.status ? getOrderStatusMeta(order.status) : { label: '未填写', color: 'default' };
  const eventDisplay = order.eventId ? formatEventDisplay(order.eventId, events) : '未填写';
  const exhibitorDisplay = order.exhibitorId ? formatExhibitorDisplay(order.exhibitorId) : '未填写';

  if (isMobile) {
    return (
      <PageContainer
        title={`订单 ${formatShortId(order.id)}`}
        extra={[
          <Tooltip key="next" title={nextAction.reason}>
            <Button type="primary" disabled={nextAction.disabled} onClick={() => navigate(nextAction.to)}>
              {nextAction.label}
            </Button>
          </Tooltip>,
        ]}
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <ProCard>
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <StatusTag meta={orderStatusMeta} />
              <Typography.Text>{eventDisplay}</Typography.Text>
              <Typography.Text type="secondary">
                {formatArea(order.areaSqm)} · {formatMobileText(order.boothType)} · {formatMobileAmount(order.totalAmount)}
              </Typography.Text>
            </Space>
          </ProCard>

          <ProCard title="参展进度">
            <Steps current={getOrderStepIndex(order.status)} direction="vertical" items={buildHomeFlowSteps()} />
          </ProCard>

          <Collapse
            defaultActiveKey={['basic', 'timeline']}
            items={[
              {
                key: 'basic',
                label: '基本信息',
                children: (
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <Typography.Text>订单 ID：{formatMobileText(order.id)}</Typography.Text>
                    <Typography.Text>展会：{eventDisplay}</Typography.Text>
                    <Typography.Text>展商：{exhibitorDisplay}</Typography.Text>
                    <Typography.Text>展区/分区：{order.zoneId ? formatShortId(order.zoneId) : '不限分区'}</Typography.Text>
                    <Typography.Text>面积档位：{order.areaTierId ? formatAreaTierDisplay(order.areaTierId, areaTiers) : '未填写'}</Typography.Text>
                    <Typography.Text>展位类型：{formatMobileText(order.boothType)}</Typography.Text>
                    <Typography.Text>面积：{formatArea(order.areaSqm)}</Typography.Text>
                    <Typography.Text>总金额：{formatMobileAmount(order.totalAmount)}</Typography.Text>
                    <Typography.Text type="secondary">创建时间：{formatMobileTime(order.createdAt)}</Typography.Text>
                    <Typography.Text type="secondary">更新时间：{formatMobileTime(order.updatedAt)}</Typography.Text>
                  </Space>
                ),
              },
              {
                key: 'contracts',
                label: '合同信息',
                children: contracts.length ? (
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    {contracts.map((contract) => (
                      <ProCard
                        key={contract.id}
                        title={formatMobileText(contract.contractNo)}
                        extra={<StatusTag meta={contract.status ? getContractStatusMeta(contract.status) : { label: '未填写', color: 'default' }} />}
                      >
                        <Space direction="vertical" size={6}>
                          <Typography.Text>合同 ID：{formatMobileText(contract.id)}</Typography.Text>
                          <Typography.Text>当前版本：{formatMobileOptional(contract.currentVersionId)}</Typography.Text>
                          <Typography.Text type="secondary">创建时间：{formatMobileTime(contract.createdAt)}</Typography.Text>
                          <Typography.Text type="secondary">更新时间：{formatMobileTime(contract.updatedAt)}</Typography.Text>
                        </Space>
                      </ProCard>
                    ))}
                  </Space>
                ) : (
                  <Empty description="暂无" />
                ),
              },
              {
                key: 'payments',
                label: '付款记录',
                children: payments.length ? (
                  <List<PaymentRecord>
                    dataSource={payments}
                    renderItem={(payment) => {
                      const failureReason = (payment as PaymentRecord & { failureReason?: string | null }).failureReason;
                      return (
                        <List.Item style={{ paddingInline: 0 }}>
                          <ProCard title={`付款 ${formatShortId(payment.id)}`}>
                            <Space direction="vertical" size={6}>
                              <Typography.Text>金额：{formatMobileAmount(payment.amount)}</Typography.Text>
                              <Typography.Text>方式：{formatPaymentMethod(payment.method)}</Typography.Text>
                              <StatusTag meta={payment.status ? getPaymentStatusMeta(payment.status) : { label: '未填写', color: 'default' }} />
                              <Typography.Text type="secondary">提交时间：{formatMobileTime(payment.createdAt)}</Typography.Text>
                              <Typography.Text type="secondary">确认时间：{formatMobileTime(payment.confirmedAt)}</Typography.Text>
                              {payment.status === 'CONFIRM_FAILED' ? (
                                <Typography.Text type="danger">
                                  失败原因：{failureReason?.trim() ? failureReason : '暂无失败原因'}
                                </Typography.Text>
                              ) : null}
                            </Space>
                          </ProCard>
                        </List.Item>
                      );
                    }}
                  />
                ) : (
                  <Empty description="暂无" />
                ),
              },
              {
                key: 'selections',
                label: '选位记录',
                children: selections.length ? (
                  <List<BoothSelection>
                    dataSource={selections}
                    renderItem={(selection) => (
                      <List.Item style={{ paddingInline: 0 }}>
                        <ProCard title={`选位 ${formatShortId(selection.id)}`}>
                          <Space direction="vertical" size={6}>
                            <StatusTag meta={selection.status ? getSelectionStatusMeta(selection.status) : { label: '未填写', color: 'default' }} />
                            <Typography.Text>面积：{formatArea(selection.totalAreaSqm)}</Typography.Text>
                            <Typography.Text>展位数：{selection.boothIds?.length ?? 0} 个</Typography.Text>
                            <Typography.Text type="secondary">提交时间：{formatMobileTime(selection.createdAt)}</Typography.Text>
                            <Typography.Text type="secondary">审核时间：{formatMobileTime(selection.reviewedAt)}</Typography.Text>
                          </Space>
                        </ProCard>
                      </List.Item>
                    )}
                  />
                ) : (
                  <Empty description="暂无" />
                ),
              },
              {
                key: 'timeline',
                label: '业务进度',
                children: timelineEvents.length ? (
                  <Timeline
                    items={timelineEvents.map((event) => ({
                      key: event.key,
                      children: `${event.title}：${event.timeDisplay}`,
                    }))}
                  />
                ) : (
                  <Empty description="暂无" />
                ),
              },
            ]}
          />
        </Space>
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
              <StatusTag meta={orderStatusMeta} />
            </Space>
          </ProCard>
          <ProCard>
            <Statistic title="面积" value={formatArea(order.areaSqm)} />
          </ProCard>
          <ProCard>
            <Statistic title="展位类型" value={formatMobileText(order.boothType)} />
          </ProCard>
          <ProCard>
            <Statistic title="订单金额" value={formatMobileAmount(order.totalAmount)} />
          </ProCard>
        </ProCard>

        <ProCard title="参展进度">
          <Steps
            current={getOrderStepIndex(order.status)}
            items={buildHomeFlowSteps()}
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
                        title={formatMobileText(contract.contractNo)}
                        extra={<StatusTag meta={contract.status ? getContractStatusMeta(contract.status) : { label: '未填写', color: 'default' }} />}
                      >
                        <Space direction="vertical" size={8}>
                          <Typography.Text type="secondary">合同 ID：{formatMobileText(contract.id)}</Typography.Text>
                          <Typography.Text>当前版本：{formatMobileOptional(contract.currentVersionId)}</Typography.Text>
                          <Typography.Text>创建时间：{formatMobileTime(contract.createdAt)}</Typography.Text>
                          <Typography.Text>更新时间：{formatMobileTime(contract.updatedAt)}</Typography.Text>
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
