import { PageContainer, ProForm, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert, Button, message, Space, Statistic } from 'antd';
import { useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { apiGet, apiPost } from '../../../app/request';
import {
  buildBoothSelectionBody,
  canSubmitBoothSelection,
  compareAreaToOrder,
  filterBoothsForOrderType,
} from '../pageLogic';

type OrderRow = {
  id: string;
  eventId: string;
  areaSqm: number | string;
  boothType: string;
  status: string;
};

type BoothRow = {
  id: string;
  eventId: string;
  boothNo: string;
  areaSqm: number | string;
  boothType: string;
  status: string;
};

type SelectionResponse = {
  id: string;
  status: string;
};

type LoadValues = {
  orderId: string;
};

export default function BoothSelectionPage() {
  const { orderId: orderIdParam } = useParams();
  const [searchParams] = useSearchParams();
  const initialOrderId = orderIdParam ?? searchParams.get('orderId') ?? undefined;
  const [order, setOrder] = useState<OrderRow>();
  const [booths, setBooths] = useState<BoothRow[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const selectedBooths = useMemo(
    () => booths.filter((booth) => selectedRowKeys.includes(booth.id)),
    [booths, selectedRowKeys],
  );
  const areaComparison = useMemo(
    () => compareAreaToOrder(selectedBooths.map((booth) => booth.areaSqm), order?.areaSqm ?? 0),
    [order?.areaSqm, selectedBooths],
  );
  const showAreaAlert = order && selectedRowKeys.length > 0 && !areaComparison.matches;
  const canSubmit = canSubmitBoothSelection({
    loading,
    hasOrder: Boolean(order),
    areaMatches: areaComparison.matches,
    selectedCount: selectedRowKeys.length,
  });

  const columns: ProColumns<BoothRow>[] = [
    { title: '展位号', dataIndex: 'boothNo' },
    { title: '面积', dataIndex: 'areaSqm', valueType: 'digit' },
    { title: '展位类型', dataIndex: 'boothType' },
    { title: '状态', dataIndex: 'status' },
  ];

  async function loadOrderAndBooths(values: LoadValues) {
    setOrder(undefined);
    setBooths([]);
    setSelectedRowKeys([]);
    setLoading(true);
    try {
      const orders = await apiGet<OrderRow[]>(`/api/participation-orders?exhibitorId=${DEMO_EXHIBITOR_ID}`);
      const matchedOrder = orders.find((item) => item.id === values.orderId);
      if (!matchedOrder) {
        message.error('未找到当前展商的订单');
        setOrder(undefined);
        setBooths([]);
        setSelectedRowKeys([]);
        return false;
      }

      const availableBooths = await apiGet<BoothRow[]>(
        `/api/booths?eventId=${encodeURIComponent(matchedOrder.eventId)}&status=AVAILABLE`,
      );
      setOrder(matchedOrder);
      setBooths(filterBoothsForOrderType(availableBooths, matchedOrder));
      setSelectedRowKeys([]);
      return true;
    } finally {
      setLoading(false);
    }
  }

  async function submitSelection() {
    if (!order || !canSubmit) {
      return;
    }

    setSubmitting(true);
    try {
      const selection = await apiPost<SelectionResponse>(
        '/api/booth-selections',
        buildBoothSelectionBody(order.id, selectedRowKeys.map(String)),
      );
      message.success(`选位 ${selection.id} 已提交，状态：${selection.status}`);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <PageContainer title="选择展位">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProForm<LoadValues>
          layout="horizontal"
          initialValues={{ orderId: initialOrderId }}
          submitter={{ searchConfig: { submitText: '加载可选展位' } }}
          onFinish={loadOrderAndBooths}
        >
          <ProFormText name="orderId" label="订单 ID" rules={[{ required: true, message: '请输入订单 ID' }]} />
        </ProForm>

        <Space size={32} wrap>
          <Statistic title="订单面积" value={areaComparison.orderDisplay} precision={2} suffix="㎡" />
          <Statistic title="已选面积" value={areaComparison.selectedDisplay} precision={2} suffix="㎡" />
          <Statistic title="已选展位" value={selectedRowKeys.length} suffix="个" />
        </Space>

        {showAreaAlert ? (
          <Alert
            type={areaComparison.relation === 'short' ? 'warning' : 'error'}
            showIcon
            message={areaComparison.relation === 'short' ? '已选面积不足' : '已选面积超出'}
            description={`订单面积 ${areaComparison.orderFixed} ㎡，当前已选 ${areaComparison.selectedFixed} ㎡。`}
          />
        ) : null}

        <ProTable<BoothRow>
          rowKey="id"
          search={false}
          loading={loading}
          columns={columns}
          dataSource={booths}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          toolBarRender={() => [
            <Button
              key="submit"
              type="primary"
              disabled={!canSubmit}
              loading={submitting}
              onClick={submitSelection}
            >
              提交选位
            </Button>,
          ]}
        />
      </Space>
    </PageContainer>
  );
}
