import { PageContainer, ProCard, ProDescriptions, ProTable } from '@ant-design/pro-components';
import type { ProColumns, ProDescriptionsItemProps } from '@ant-design/pro-components';
import { Alert, Button, Empty, message, Modal, Result, Skeleton, Space, Statistic, Tabs, Timeline, Typography } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { BoothSelection, ContractRecord, ParticipationOrder, PaymentRecord } from '../../../app/domain';
import { formatArea, formatFriendlyTime, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import {
  getContractStatusMeta,
  getOrderStatusMeta,
  getPaymentStatusMeta,
  getSelectionStatusMeta,
  StatusTag,
} from '../../../app/status';

function byCreatedAtDesc<T extends { createdAt: string }>(left: T, right: T) {
  return Date.parse(right.createdAt) - Date.parse(left.createdAt);
}

type TimelineEntry = {
  time: string;
  title: string;
  description: string;
};

function addTimelineEntry(entries: TimelineEntry[], time: string | null | undefined, title: string, description: string) {
  if (!time) return;
  entries.push({ time, title, description });
}

export default function AdminOrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const requestSeq = useRef(0);
  const [loading, setLoading] = useState(true);
  const [relatedLoadFailed, setRelatedLoadFailed] = useState(false);
  const [order, setOrder] = useState<ParticipationOrder>();
  const [contracts, setContracts] = useState<ContractRecord[]>([]);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [selections, setSelections] = useState<BoothSelection[]>([]);

  const loadOrderDetail = useCallback(async () => {
    const requestId = requestSeq.current + 1;
    requestSeq.current = requestId;

    if (!orderId) {
      setOrder(undefined);
      setContracts([]);
      setPayments([]);
      setSelections([]);
      setRelatedLoadFailed(false);
      setLoading(false);
      return;
    }

    const encodedOrderId = encodeURIComponent(orderId);
    setLoading(true);
    setRelatedLoadFailed(false);
    try {
      const loadedOrder = await apiGet<ParticipationOrder>(`/api/participation-orders/${encodedOrderId}`);
      if (requestSeq.current !== requestId) return;

      setOrder(loadedOrder);

      const [contractsResult, paymentsResult, selectionsResult] = await Promise.allSettled([
        apiGet<ContractRecord[]>(`/api/contracts?orderId=${encodedOrderId}`),
        apiGet<PaymentRecord[]>(`/api/payments?orderId=${encodedOrderId}`),
        apiGet<BoothSelection[]>(`/api/booth-selections?orderId=${encodedOrderId}`),
      ] as const);
      if (requestSeq.current !== requestId) return;

      setContracts(contractsResult.status === 'fulfilled' ? [...contractsResult.value].sort(byCreatedAtDesc) : []);
      setPayments(paymentsResult.status === 'fulfilled' ? [...paymentsResult.value].sort(byCreatedAtDesc) : []);
      setSelections(selectionsResult.status === 'fulfilled' ? [...selectionsResult.value].sort(byCreatedAtDesc) : []);
      setRelatedLoadFailed(
        contractsResult.status === 'rejected' ||
          paymentsResult.status === 'rejected' ||
          selectionsResult.status === 'rejected',
      );
    } catch {
      if (requestSeq.current !== requestId) return;
      setOrder(undefined);
      setContracts([]);
      setPayments([]);
      setSelections([]);
      setRelatedLoadFailed(false);
    } finally {
      if (requestSeq.current === requestId) setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    loadOrderDetail();
    return () => {
      requestSeq.current += 1;
    };
  }, [loadOrderDetail]);

  const basicColumns = useMemo<ProDescriptionsItemProps<ParticipationOrder>[]>(
    () => [
      { title: '订单 ID', dataIndex: 'id', copyable: true, ellipsis: true },
      { title: '展会 ID', dataIndex: 'eventId', ellipsis: true },
      { title: '展商 ID', dataIndex: 'exhibitorId', ellipsis: true },
      { title: '展区 ID', dataIndex: 'zoneId', renderText: (value) => value || '-' },
      { title: '面积档位', dataIndex: 'areaTierId', ellipsis: true },
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
    [],
  );

  const contractColumns: ProColumns<ContractRecord>[] = [
    { title: '合同号', dataIndex: 'contractNo', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getContractStatusMeta(record.status)} />,
    },
    {
      title: '当前版本',
      dataIndex: 'currentVersionId',
      ellipsis: true,
      render: (_, record) => formatShortId(record.currentVersionId),
    },
    { title: '创建时间', dataIndex: 'createdAt', render: (_, record) => formatFriendlyTime(record.createdAt) },
    { title: '更新时间', dataIndex: 'updatedAt', render: (_, record) => formatFriendlyTime(record.updatedAt) },
  ];

  const paymentColumns: ProColumns<PaymentRecord>[] = [
    {
      title: '付款',
      dataIndex: 'id',
      ellipsis: true,
      render: (_, record) => `付款 ${formatShortId(record.id)}`,
    },
    { title: '金额', dataIndex: 'amount', render: (_, record) => formatMoney(record.amount) },
    { title: '方式', dataIndex: 'method' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getPaymentStatusMeta(record.status)} />,
    },
    { title: '提交时间', dataIndex: 'createdAt', render: (_, record) => formatFriendlyTime(record.createdAt) },
    { title: '确认时间', dataIndex: 'confirmedAt', render: (_, record) => formatFriendlyTime(record.confirmedAt) },
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
    { title: '面积', dataIndex: 'totalAreaSqm', render: (_, record) => formatArea(record.totalAreaSqm) },
    { title: '展位数', dataIndex: 'boothIds', render: (_, record) => `${record.boothIds.length} 个` },
    { title: '提交时间', dataIndex: 'createdAt', render: (_, record) => formatFriendlyTime(record.createdAt) },
    { title: '审核时间', dataIndex: 'reviewedAt', render: (_, record) => formatFriendlyTime(record.reviewedAt) },
    { title: '审核备注', dataIndex: 'reviewComment', ellipsis: true, renderText: (value) => value || '-' },
  ];

  const timelineItems = useMemo(() => {
    const entries: TimelineEntry[] = [];

    contracts.forEach((contract) => {
      addTimelineEntry(entries, contract.createdAt, `合同 ${contract.contractNo} 生成`, `状态：${getContractStatusMeta(contract.status).label}`);
      if (contract.updatedAt !== contract.createdAt) {
        addTimelineEntry(entries, contract.updatedAt, `合同 ${contract.contractNo} 更新`, `状态：${getContractStatusMeta(contract.status).label}`);
      }
    });

    payments.forEach((payment) => {
      addTimelineEntry(entries, payment.createdAt, `付款 ${formatShortId(payment.id)} 提交`, `金额：${formatMoney(payment.amount)}`);
      addTimelineEntry(entries, payment.confirmedAt, `付款 ${formatShortId(payment.id)} 确认`, `状态：${getPaymentStatusMeta(payment.status).label}`);
    });

    selections.forEach((selection) => {
      addTimelineEntry(entries, selection.createdAt, `选位 ${formatShortId(selection.id)} 提交`, `面积：${formatArea(selection.totalAreaSqm)}`);
      addTimelineEntry(entries, selection.reviewedAt, `选位 ${formatShortId(selection.id)} 审核`, `状态：${getSelectionStatusMeta(selection.status).label}`);
    });

    return entries
      .sort((left, right) => Date.parse(right.time) - Date.parse(left.time))
      .map((entry) => ({
        children: (
          <Space direction="vertical" size={2}>
            <Typography.Text strong>{entry.title}</Typography.Text>
            <Typography.Text type="secondary">{entry.description}</Typography.Text>
            <Typography.Text type="secondary">{formatFriendlyTime(entry.time)}</Typography.Text>
          </Space>
        ),
      }));
  }, [contracts, payments, selections]);

  const showSalesConfirm = useCallback(() => {
    if (!order) return;
    Modal.confirm({
      title: '确认销售审核通过？',
      content: `订单 ${formatShortId(order.id)} 确认后将生成合同。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        await apiPost<ParticipationOrder>(`/api/participation-orders/${order.id}/confirm`);
        message.success('销售确认已完成');
        await loadOrderDetail();
      },
    });
  }, [loadOrderDetail, order]);

  if (loading) {
    return (
      <PageContainer title="订单详情">
        <Skeleton active paragraph={{ rows: 10 }} />
      </PageContainer>
    );
  }

  if (!order) {
    return (
      <PageContainer title="订单详情">
        <ProCard>
          <Result
            status="404"
            title="未找到订单"
            subTitle={`订单 ${orderId ?? '-'} 不存在或暂时无法访问。`}
            extra={
              <Button type="primary" onClick={() => navigate('/admin/orders')}>
                返回订单管理
              </Button>
            }
          />
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title={`订单 ${formatShortId(order.id)}`}
      extra={
        order.status === 'PENDING_SALES_CONFIRM'
          ? [
              <Button key="confirm" type="primary" onClick={showSalesConfirm}>
                销售确认
              </Button>,
            ]
          : []
      }
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        {relatedLoadFailed ? <Alert showIcon type="warning" message="部分关联信息加载失败，请稍后重试" /> : null}

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
                children: (
                  <ProTable<ContractRecord>
                    rowKey="id"
                    search={false}
                    options={false}
                    pagination={false}
                    columns={contractColumns}
                    dataSource={contracts}
                    locale={{ emptyText: '暂无合同' }}
                  />
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
                  />
                ),
              },
              {
                key: 'timeline',
                label: '操作时间线',
                children: timelineItems.length ? <Timeline items={timelineItems} /> : <Empty description="暂无操作记录" />,
              },
            ]}
          />
        </ProCard>
      </Space>
    </PageContainer>
  );
}
