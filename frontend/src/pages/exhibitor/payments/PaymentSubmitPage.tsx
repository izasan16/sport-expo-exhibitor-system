import { PageContainer, ProCard, ProDescriptions, ProForm, ProFormDigit, ProFormSelect, ProTable } from '@ant-design/pro-components';
import type { ProColumns, ProDescriptionsItemProps } from '@ant-design/pro-components';
import { Button, message, Result, Skeleton, Space, Statistic } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { ParticipationOrder, PaymentRecord } from '../../../app/domain';
import { formatFriendlyTime, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { getOrderStatusMeta, getPaymentStatusMeta, StatusTag } from '../../../app/status';
import { isOrderPayable } from '../pageLogic';

type PaymentValues = {
  amount: number;
  method: string;
};

type PaymentResponse = {
  id: string;
  status: string;
};

function byCreatedAtDesc<T extends { createdAt: string }>(left: T, right: T) {
  return Date.parse(right.createdAt) - Date.parse(left.createdAt);
}

function hasAtMostTwoDecimals(value: unknown) {
  return /^\d+(\.\d{1,2})?$/.test(String(value));
}

export default function PaymentSubmitPage() {
  const { orderId: orderIdParam } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const orderId = orderIdParam ?? searchParams.get('orderId') ?? undefined;
  const [loading, setLoading] = useState(Boolean(orderId));
  const [order, setOrder] = useState<ParticipationOrder>();
  const [payments, setPayments] = useState<PaymentRecord[]>([]);

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
      try {
        const [loadedOrder, loadedPayments] = await Promise.all([
          apiGet<ParticipationOrder>(`/api/participation-orders/${orderId}`),
          apiGet<PaymentRecord[]>(`/api/payments?orderId=${orderId}`),
        ]);

        if (!alive) return;
        setOrder(loadedOrder);
        setPayments([...loadedPayments].sort(byCreatedAtDesc));
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
      { title: '订单状态', dataIndex: 'status', render: (_, record) => <StatusTag meta={getOrderStatusMeta(record.status)} /> },
      { title: '展位类型', dataIndex: 'boothType' },
      { title: '总金额', dataIndex: 'totalAmount', render: (_, record) => formatMoney(record.totalAmount) },
    ],
    [],
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
            <Statistic title="待提交订单金额" value={formatMoney(order.totalAmount)} />
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
              options={[{ label: 'OFFLINE_TRANSFER', value: 'OFFLINE_TRANSFER' }]}
            />
          </ProForm>
        </ProCard>

        <ProCard title="已有付款记录">
          <ProTable<PaymentRecord>
            rowKey="id"
            search={false}
            options={false}
            pagination={false}
            columns={paymentColumns}
            dataSource={payments}
            locale={{ emptyText: '暂无付款记录' }}
          />
        </ProCard>
      </Space>
    </PageContainer>
  );
}
