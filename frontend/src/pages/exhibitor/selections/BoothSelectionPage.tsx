import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert, Button, Checkbox, Empty, Input, List, message, Modal, Result, Skeleton, Space, Statistic, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import type React from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { loadSelectionEligibility } from '../../../app/apiLoaders';
import type { Booth, ParticipationOrder, SelectionEligibility } from '../../../app/domain';
import { formatArea } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { useIsMobile } from '../../../app/responsive';
import { getBoothStatusMeta, StatusTag } from '../../../app/status';
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
  const [eligibility, setEligibility] = useState<SelectionEligibility>();
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [remark, setRemark] = useState('');
  const [loading, setLoading] = useState(Boolean(orderId));
  const [loadFailed, setLoadFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [reloadSignal, setReloadSignal] = useState(0);
  const isMobile = useIsMobile();

  useEffect(() => {
    let alive = true;

    async function loadOrderAndBooths() {
      if (!orderId) {
        setOrder(undefined);
        setBooths([]);
        setEligibility(undefined);
        setSelectedKeys([]);
        setLoadFailed(false);
        setLoading(false);
        return;
      }

      setLoading(true);
      setLoadFailed(false);
      setOrder(undefined);
      setBooths([]);
      setEligibility(undefined);
      setSelectedKeys([]);

      try {
        const loadedOrder = await apiGet<ParticipationOrder>(`/api/participation-orders/${encodeURIComponent(orderId)}`);
        const [availableBooths, loadedEligibility] = await Promise.all([
          apiGet<Booth[]>(`/api/booths?eventId=${encodeURIComponent(loadedOrder.eventId)}&status=AVAILABLE`),
          loadSelectionEligibility(loadedOrder.id),
        ]);

        if (!alive) return;
        setOrder(loadedOrder);
        setBooths(filterBoothsForOrderType(availableBooths, loadedOrder));
        setEligibility(loadedEligibility);
      } catch {
        if (!alive) return;
        setLoadFailed(true);
        setOrder(undefined);
        setBooths([]);
        setEligibility(undefined);
        setSelectedKeys([]);
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadOrderAndBooths();
    return () => {
      alive = false;
    };
  }, [orderId, reloadSignal]);

  const sortedBooths = useMemo(() => sortBoothsForMap(booths), [booths]);
  const selectedBooths = useMemo(
    () => booths.filter((booth) => selectedKeys.includes(booth.id)),
    [booths, selectedKeys],
  );
  const areaComparison = useMemo(
    () => compareAreaToOrder(selectedBooths.map((booth) => booth.areaSqm), order?.areaSqm ?? 0),
    [order?.areaSqm, selectedBooths],
  );
  const showAreaAlert = order && selectedKeys.length > 0 && !areaComparison.matches;
  const isEligibleForSelection = order?.status === 'PAID' && eligibility?.status === 'AVAILABLE';
  const canSubmit =
    isEligibleForSelection &&
    canSubmitBoothSelection({
      loading,
      hasOrder: Boolean(order),
      areaMatches: areaComparison.matches,
      selectedCount: selectedKeys.length,
    });

  const columns: ProColumns<Booth>[] = [
    { title: '展位号', dataIndex: 'boothNo' },
    { title: '面积', dataIndex: 'areaSqm', render: (_, record) => formatArea(record.areaSqm) },
    { title: '展位类型', dataIndex: 'boothType' },
    { title: '状态', dataIndex: 'status', render: (_, record) => <StatusTag meta={getBoothStatusMeta(record.status)} /> },
  ];

  function toggleBooth(boothId: string, checked: boolean) {
    setSelectedKeys((current) => (
      checked
        ? Array.from(new Set([...current, boothId]))
        : current.filter((key) => key !== boothId)
    ));
  }

  function submitSelection() {
    if (!order || !canSubmit) {
      return;
    }

    const boothIds = selectedKeys.map(String);
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
            buildBoothSelectionBody(order.id, boothIds, remark),
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

  if (loadFailed) {
    return (
      <PageContainer title="选择展位">
        <Result
          status="error"
          title="加载展位信息失败"
          subTitle="网络或服务暂时异常，请稍后重试，或返回订单列表重新进入选位。"
          extra={
            <Space>
              <Button type="primary" onClick={() => setReloadSignal((current) => current + 1)}>
                重试
              </Button>
              <Button onClick={() => navigate('/exhibitor/orders')}>返回我的订单</Button>
            </Space>
          }
        />
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
          <Statistic title="已选展位" value={selectedKeys.length} suffix="个" />
        </Space>

        {showAreaAlert ? (
          <Alert
            type={areaComparison.relation === 'short' ? 'warning' : 'error'}
            showIcon
            message={areaComparison.relation === 'short' ? '已选面积不足' : '已选面积超出'}
            description={`订单面积 ${areaComparison.orderFixed} ㎡，当前已选 ${areaComparison.selectedFixed} ㎡。`}
          />
        ) : null}

        {!isEligibleForSelection ? (
          <Alert
            type="warning"
            showIcon
            message="当前订单暂不可选位"
            description="订单需已付款，且选位资格已开放后才能提交展位选择。"
          />
        ) : null}

        <Input.TextArea
          value={remark}
          onChange={(event) => setRemark(event.target.value)}
          placeholder="备注"
          autoSize={{ minRows: 2, maxRows: 4 }}
          maxLength={200}
          showCount
        />

        {isMobile ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <List<Booth>
              header="可选展位"
              dataSource={sortedBooths}
              locale={{ emptyText: <Empty description="暂无可选展位" /> }}
              renderItem={(booth) => (
                <List.Item style={{ paddingInline: 0 }}>
                  <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
                    <Space direction="vertical" size={4}>
                      <Typography.Text strong>{booth.boothNo}</Typography.Text>
                      <Typography.Text>{formatArea(booth.areaSqm)} · {booth.boothType}</Typography.Text>
                      <StatusTag meta={getBoothStatusMeta(booth.status)} />
                    </Space>
                    <Checkbox
                      checked={selectedKeys.includes(booth.id)}
                      onChange={(event) => toggleBooth(booth.id, event.target.checked)}
                    />
                  </Space>
                </List.Item>
              )}
            />

            <List<Booth>
              header="已选展位"
              dataSource={selectedBooths}
              locale={{ emptyText: <Empty description="暂无已选展位" /> }}
              renderItem={(booth) => (
                <List.Item
                  actions={[
                    <Button key="cancel" size="small" onClick={() => toggleBooth(booth.id, false)}>
                      取消
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={booth.boothNo}
                    description={`${formatArea(booth.areaSqm)} · ${booth.boothType}`}
                  />
                </List.Item>
              )}
            />

            <Button type="primary" block disabled={!canSubmit} loading={submitting} onClick={submitSelection}>
              提交选位
            </Button>
          </Space>
        ) : (
          <ProTable<Booth>
            headerTitle="可选展位"
            rowKey="id"
            search={false}
            loading={loading}
            columns={columns}
            dataSource={sortedBooths}
            rowSelection={{
              selectedRowKeys: selectedKeys,
              onChange: setSelectedKeys,
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
        )}
      </Space>
    </PageContainer>
  );
}
