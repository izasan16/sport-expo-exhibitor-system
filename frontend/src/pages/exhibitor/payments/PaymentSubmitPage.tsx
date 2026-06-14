import { PageContainer, ProCard, ProDescriptions, ProForm, ProFormDigit, ProFormSelect, ProTable } from '@ant-design/pro-components';
import type { ProColumns, ProDescriptionsItemProps } from '@ant-design/pro-components';
import { Button, Empty, List, message, Result, Skeleton, Space, Statistic, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { formatPaymentMethod } from '../../../app/display';
import type { ParticipationOrder, PaymentRecord } from '../../../app/domain';
import { formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { useIsMobile } from '../../../app/responsive';
import { getOrderStatusMeta, getPaymentStatusMeta, StatusTag } from '../../../app/status';
import { formatMobileAmount, formatMobileText, formatMobileTime } from '../mobileViewModels';
import { getExhibitorLoadErrorCopy, isOrderPayable } from '../pageLogic';

type PaymentValues = {
  amount: number;
  method: string;
};

type PaymentResponse = {
  id: string;
  status: string;
};

type PaymentRecordWithFailure = PaymentRecord & {
  failureReason?: string | null;
};

function byCreatedAtDesc<T extends { createdAt?: string | null }>(left: T, right: T) {
  return (Date.parse(right.createdAt ?? '') || 0) - (Date.parse(left.createdAt ?? '') || 0);
}

function hasAtMostTwoDecimals(value: unknown) {
  return /^\d+(\.\d{1,2})?$/.test(String(value));
}

export default function PaymentSubmitPage() {
  const { orderId: orderIdParam } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const orderId = orderIdParam ?? searchParams.get('orderId') ?? undefined;
  const isMobile = useIsMobile();
  const [loading, setLoading] = useState(Boolean(orderId));
  const [order, setOrder] = useState<ParticipationOrder>();
  const [payments, setPayments] = useState<PaymentRecordWithFailure[]>([]);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    let alive = true;

    async function load() {
      if (!orderId) {
        setLoading(false);
        setOrder(undefined);
        setPayments([]);
        return;
      }

      setLoading(true);
      setLoadError(false);
      try {
        const [loadedOrder, loadedPayments] = await Promise.all([
          apiGet<ParticipationOrder>(`/api/participation-orders/${orderId}`),
          apiGet<PaymentRecordWithFailure[]>(`/api/payments?orderId=${orderId}`),
        ]);

        if (!alive) return;
        setOrder(loadedOrder);
        setPayments([...loadedPayments].sort(byCreatedAtDesc));
      } catch {
        if (!alive) return;
        setOrder(undefined);
        setPayments([]);
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

  const orderColumns = useMemo<ProDescriptionsItemProps<ParticipationOrder>[]>(
    () => [
      { title: '订单', dataIndex: 'id', render: (_, record) => `订单 ${formatShortId(record.id)}` },
      { title: '订单状态', dataIndex: 'status', render: (_, record) => <StatusTag meta={record.status ? getOrderStatusMeta(record.status) : { label: '未填写', color: 'default' }} /> },
      { title: '展位类型', dataIndex: 'boothType', render: (_, record) => formatMobileText(record.boothType) },
      { title: '总金额', dataIndex: 'totalAmount', render: (_, record) => formatMobileAmount(record.totalAmount) },
    ],
    [],
  );

  const paymentColumns: ProColumns<PaymentRecordWithFailure>[] = [
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
  ];
  const paymentMethodOptions = [
    { label: '银行转账', value: 'OFFLINE_TRANSFER' },
    { label: '现金', value: 'OFFLINE_CASH' },
    { label: '支票', value: 'OFFLINE_CHECK' },
  ];

  if (!orderId) {
    return (
      <PageContainer title="提交付款">
        <ProCard>
          <Result
            status="warning"
            title="缺少订单 ID"
            subTitle="请从订单详情页发起付款提交。"
            extra={
              <Button type="primary" onClick={() => navigate('/exhibitor/orders')}>
                返回我的订单
              </Button>
            }
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (loading) {
    return (
      <PageContainer title="提交付款">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (loadError) {
    const copy = getExhibitorLoadErrorCopy('payment');
    return (
      <PageContainer title="提交付款">
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
      <PageContainer title="提交付款">
        <ProCard>
          <Result
            status="404"
            title="未找到订单"
            extra={
              <Button type="primary" onClick={() => navigate('/exhibitor/orders')}>
                返回我的订单
              </Button>
            }
          />
        </ProCard>
      </PageContainer>
    );
  }

  if (!isOrderPayable(order.status)) {
    return (
      <PageContainer title="提交付款">
        <ProCard>
          <Result
            status="info"
            title="当前订单无需提交付款"
            subTitle="只有待付款或部分付款订单可以继续提交付款。"
            extra={
              <Button type="primary" onClick={() => navigate(`/exhibitor/orders/${order.id}`)}>
                返回订单详情
              </Button>
            }
          />
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="提交付款">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard split="vertical">
          <ProCard title="订单信息">
            <ProDescriptions<ParticipationOrder> column={2} dataSource={order} columns={orderColumns} />
          </ProCard>
          <ProCard>
            <Statistic title="待提交订单金额" value={formatMobileAmount(order.totalAmount)} />
          </ProCard>
        </ProCard>

        <ProCard title="付款信息">
          <ProForm<PaymentValues>
            layout="horizontal"
            initialValues={{ method: 'OFFLINE_TRANSFER' }}
            submitter={{ searchConfig: { submitText: '提交付款' } }}
            onFinish={async (values) => {
              const payment = await apiPost<PaymentResponse>('/api/payments', {
                orderId,
                amount: values.amount,
                method: values.method,
              });
              message.success(`付款 ${payment.id} 已提交，状态：${payment.status}`);
              navigate(`/exhibitor/orders/${orderId}`);
              return true;
            }}
          >
            <ProFormDigit
              name="amount"
              label="付款金额"
              min={0.01}
              fieldProps={{ precision: 2 }}
              rules={[
                { required: true, message: '请输入付款金额' },
                {
                  validator: async (_rule: unknown, value: unknown) => {
                    if (value === undefined || value === null || value === '') return;
                    if (!hasAtMostTwoDecimals(value)) {
                      throw new Error('付款金额最多保留两位小数');
                    }
                  },
                },
              ]}
            />
            <ProFormSelect
              name="method"
              label="付款方式"
              initialValue="OFFLINE_TRANSFER"
              rules={[{ required: true, message: '请选择付款方式' }]}
              options={paymentMethodOptions}
            />
          </ProForm>
        </ProCard>

        <ProCard title="已有付款记录">
          {isMobile ? (
            <List<PaymentRecordWithFailure>
              dataSource={payments}
              locale={{ emptyText: <Empty description="暂无付款记录" /> }}
              renderItem={(payment) => (
                <List.Item style={{ paddingInline: 0 }}>
                  <ProCard title={`付款 ${formatShortId(payment.id)}`}>
                    <Space direction="vertical" size={6}>
                      <Typography.Text>金额：{formatMobileAmount(payment.amount)}</Typography.Text>
                      <Typography.Text>方式：{formatPaymentMethod(payment.method)}</Typography.Text>
                      <StatusTag meta={payment.status ? getPaymentStatusMeta(payment.status) : { label: '未填写', color: 'default' }} />
                      <Typography.Text type="secondary">提交时间：{formatMobileTime(payment.createdAt)}</Typography.Text>
                      {payment.status === 'CONFIRM_FAILED' ? (
                        <Typography.Text type="danger">
                          失败原因：{payment.failureReason?.trim() ? payment.failureReason : '暂无失败原因'}
                        </Typography.Text>
                      ) : null}
                    </Space>
                  </ProCard>
                </List.Item>
              )}
            />
          ) : (
            <ProTable<PaymentRecordWithFailure>
              rowKey="id"
              search={false}
              options={false}
              pagination={false}
              columns={paymentColumns}
              dataSource={payments}
              locale={{ emptyText: '暂无付款记录' }}
            />
          )}
        </ProCard>
      </Space>
    </PageContainer>
  );
}
