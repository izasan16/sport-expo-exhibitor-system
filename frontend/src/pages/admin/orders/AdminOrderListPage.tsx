import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Space } from 'antd';
import { useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { DEMO_EXHIBITOR_ID } from '../../../app/demoActors';
import { buildValueEnum, formatEventDisplay, formatExhibitorDisplay } from '../../../app/display';
import type { ParticipationOrder } from '../../../app/domain';
import { formatArea, formatMoney, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { getOrderStatusMeta, StatusTag } from '../../../app/status';
import { useEvents } from '../../../app/useEvents';

function buildQuery(params: Record<string, unknown>) {
  const searchParams = new URLSearchParams();
  ['eventId', 'exhibitorId', 'status'].forEach((key) => {
    const value = params[key];
    if (value) {
      searchParams.set(key, String(value));
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

export default function AdminOrderListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const statusHint = searchParams.get('status') ?? undefined;
  const actionRef = useRef<ActionType>();
  const { events, eventValueEnum } = useEvents();
  const exhibitorValueEnum = buildValueEnum([{ label: '演示展商', value: DEMO_EXHIBITOR_ID }]);
  const statusValueEnum = Object.fromEntries(
    ['PENDING_SALES_CONFIRM', 'PENDING_CONTRACT', 'PENDING_PAYMENT', 'PARTIALLY_PAID', 'PAID', 'COMPLETED']
      .map((status) => [status, { text: getOrderStatusMeta(status).label }]),
  );

  const columns: ProColumns<ParticipationOrder>[] = [
    {
      title: '订单',
      dataIndex: 'id',
      ellipsis: true,
      hideInSearch: true,
      render: (_, record) => formatShortId(record.id),
    },
    {
      title: '展会',
      dataIndex: 'eventId',
      valueType: 'select',
      valueEnum: eventValueEnum,
      render: (_, record) => formatEventDisplay(record.eventId, events),
    },
    {
      title: '展商',
      dataIndex: 'exhibitorId',
      valueType: 'select',
      valueEnum: exhibitorValueEnum,
      render: (_, record) => formatExhibitorDisplay(record.exhibitorId),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      render: (_, record) => <StatusTag meta={getOrderStatusMeta(record.status)} />,
    },
    { title: '展位类型', dataIndex: 'boothType', hideInSearch: true },
    {
      title: '面积',
      dataIndex: 'areaSqm',
      hideInSearch: true,
      render: (_, record) => formatArea(record.areaSqm),
    },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      hideInSearch: true,
      render: (_, record) => formatMoney(record.totalAmount),
    },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime', hideInSearch: true },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => {
        const actions = [
          <Button key="detail" type="link" size="small" onClick={() => navigate(`/admin/orders/${record.id}`)}>
            查看详情
          </Button>,
        ];

        if (record.status === 'PENDING_SALES_CONFIRM') {
          actions.push(
            <Button
              key="confirm"
              type="link"
              size="small"
              onClick={() => {
                Modal.confirm({
                  title: '确认销售审核通过？',
                  content: `订单 ${formatShortId(record.id)} 确认后将生成合同。`,
                  okText: '确认',
                  cancelText: '取消',
                  onOk: async () => {
                    await apiPost<ParticipationOrder>(`/api/participation-orders/${record.id}/confirm`);
                    message.success('销售确认已完成');
                    actionRef.current?.reload();
                  },
                });
              }}
            >
              销售确认
            </Button>,
          );
        }

        return <Space size={8}>{actions}</Space>;
      },
    },
  ];

  return (
    <PageContainer title="订单管理">
      <ProTable<ParticipationOrder>
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        params={{ status: statusHint }}
        form={{ initialValues: { status: statusHint } }}
        columns={columns}
        request={async (params) => {
          const data = await apiGet<ParticipationOrder[]>(`/api/participation-orders${buildQuery(params)}`);
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
