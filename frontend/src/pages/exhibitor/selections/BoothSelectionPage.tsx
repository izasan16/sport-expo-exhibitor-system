import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert, Button, message, Modal, Result, Skeleton, Space, Statistic } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import type { Key } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { Booth, ParticipationOrder } from '../../../app/domain';
import { formatArea } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import {
  buildBoothSelectionBody,
  canSubmitBoothSelection,
  compareAreaToOrder,
  filterBoothsForOrderType,
  sortBoothsForMap,
} from '../pageLogic';

type SelectionResponse = {
  id: string;
  status: string;
};

export default function BoothSelectionPage() {
  const { orderId: orderIdParam } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const orderId = orderIdParam ?? searchParams.get('orderId') ?? undefined;
  const [order, setOrder] = useState<ParticipationOrder>();
  const [booths, setBooths] = useState<Booth[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [loading, setLoading] = useState(Boolean(orderId));
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let alive = true;

    async function loadOrderAndBooths() {
      if (!orderId) {
        setOrder(undefined);
        setBooths([]);
        setSelectedRowKeys([]);
        setLoading(false);
        return;
      }

      setLoading(true);
      setOrder(undefined);
      setBooths([]);
      setSelectedRowKeys([]);

      try {
        const loadedOrder = await apiGet<ParticipationOrder>(`/api/participation-orders/${encodeURIComponent(orderId)}`);
        const availableBooths = await apiGet<Booth[]>(
          `/api/booths?eventId=${encodeURIComponent(loadedOrder.eventId)}&status=AVAILABLE`,
        );

        if (!alive) return;
        setOrder(loadedOrder);
        setBooths(filterBoothsForOrderType(availableBooths, loadedOrder));
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadOrderAndBooths();
    return () => {
      alive = false;
    };
  }, [orderId]);

  const sortedBooths = useMemo(() => sortBoothsForMap(booths), [booths]);
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

  const columns: ProColumns<Booth>[] = [
    { title: '展位号', dataIndex: 'boothNo' },
    { title: '面积', dataIndex: 'areaSqm', render: (_, record) => formatArea(record.areaSqm) },
    { title: '展位类型', dataIndex: 'boothType' },
    { title: '状态', dataIndex: 'status' },
  ];

  function submitSelection() {
    if (!order || !canSubmit) {
      return;
    }

    const boothIds = selectedRowKeys.map(String);
    Modal.confirm({
      title: '确认提交选位',
      content: `将提交 ${boothIds.length} 个展位，合计 ${areaComparison.selectedFixed} ㎡。提交后将进入审核流程。`,
      okText: '确认提交',
      cancelText: '再看看',
      onOk: async () => {
        setSubmitting(true);
        try {
          const selection = await apiPost<SelectionResponse>(
            '/api/booth-selections',
            buildBoothSelectionBody(order.id, boothIds),
          );
          message.success(`选位 ${selection.id} 已提交，状态：${selection.status}`);
          navigate(`/exhibitor/orders/${order.id}`);
        } finally {
          setSubmitting(false);
        }
      },
    });
  }

  if (!orderId) {
    return (
      <PageContainer title="选择展位">
        <Result
          status="warning"
          title="缺少订单 ID"
          subTitle="请从订单详情页进入展位选择。"
          extra={
            <Button type="primary" onClick={() => navigate('/exhibitor/orders')}>
              返回我的订单
            </Button>
          }
        />
      </PageContainer>
    );
  }

  if (loading) {
    return (
      <PageContainer title="选择展位">
        <Skeleton active paragraph={{ rows: 8 }} />
      </PageContainer>
    );
  }

  if (!order) {
    return (
      <PageContainer title="选择展位">
        <Result
          status="404"
          title="未找到订单"
          subTitle="请返回订单列表后重新选择。"
          extra={
            <Button type="primary" onClick={() => navigate('/exhibitor/orders')}>
              返回我的订单
            </Button>
          }
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="选择展位">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
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

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(112px, 1fr))', gap: 8 }}>
          {sortBoothsForMap(booths).map((booth) => {
            const selected = selectedRowKeys.includes(booth.id);
            return (
              <Button
                key={booth.id}
                title={`${booth.boothNo} / ${formatArea(booth.areaSqm)}`}
                type={selected ? 'primary' : 'default'}
                onClick={() => {
                  setSelectedRowKeys((current) =>
                    current.includes(booth.id)
                      ? current.filter((key) => key !== booth.id)
                      : [...current, booth.id],
                  );
                }}
                style={{ minHeight: 72, whiteSpace: 'normal', overflow: 'hidden' }}
              >
                <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{booth.boothNo}</div>
                <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{formatArea(booth.areaSqm)}</div>
              </Button>
            );
          })}
        </div>

        <ProTable<Booth>
          rowKey="id"
          search={false}
          loading={loading}
          columns={columns}
          dataSource={sortedBooths}
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
