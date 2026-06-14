import { ModalForm, PageContainer, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Space } from 'antd';
import { useRef, useState } from 'react';
import { notifyAdminDashboardStale } from '../../../app/apiLoaders';
import { formatEventDisplay, formatExhibitorDisplay } from '../../../app/display';
import type { Booth, BoothSelection } from '../../../app/domain';
import { formatArea, formatFriendlyTime, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { getSelectionStatusMeta, StatusTag } from '../../../app/status';
import { useEvents } from '../../../app/useEvents';
import {
  ADMIN_REASON_MAX_LENGTH,
  buildAdminReasonRules,
  buildBoothNoMap,
  formatNullableAdminText,
  formatSelectionBoothNumbers,
} from '../pageLogic';

type SelectionRow = BoothSelection & {
  boothNumbersDisplay: string;
};

export default function SelectionReviewPage() {
  const actionRef = useRef<ActionType>();
  const [loadingId, setLoadingId] = useState<string>();
  const [rejectLoadingId, setRejectLoadingId] = useState<string>();
  const { events } = useEvents();

  const columns: ProColumns<SelectionRow>[] = [
    { title: '展会', dataIndex: 'eventId', render: (_, record) => formatEventDisplay(record.eventId, events) },
    { title: '订单', dataIndex: 'orderId', ellipsis: true, render: (_, record) => formatShortId(record.orderId) },
    { title: '展商', dataIndex: 'exhibitorId', ellipsis: true, render: (_, record) => formatExhibitorDisplay(record.exhibitorId) },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getSelectionStatusMeta(record.status)} />,
    },
    { title: '总面积', dataIndex: 'totalAreaSqm', render: (_, record) => formatArea(record.totalAreaSqm) },
    { title: '已选展位', dataIndex: 'boothNumbersDisplay' },
    { title: '展商备注', dataIndex: 'exhibitorRemark', ellipsis: true, render: (_, record) => formatNullableAdminText(record.exhibitorRemark) },
    { title: '推荐展位', dataIndex: 'recommendedBooths', ellipsis: true, render: (_, record) => formatNullableAdminText(record.recommendedBooths) },
    { title: '锁定到期', dataIndex: 'lockExpiresAt', render: (_, record) => (record.lockExpiresAt ? formatFriendlyTime(record.lockExpiresAt) : '时间未记录') },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            loading={loadingId === record.id}
            onClick={() => {
              Modal.confirm({
                title: '确认审核通过？',
                content: `选位 ${formatShortId(record.id)} 将确认 ${formatArea(record.totalAreaSqm)} 面积。`,
                okText: '审核通过',
                cancelText: '取消',
                onOk: async () => {
                  setLoadingId(record.id);
                  try {
                    await apiPost<SelectionRow>(`/api/booth-selections/${record.id}/approve`);
                    message.success('选位审核已通过');
                    notifyAdminDashboardStale();
                    actionRef.current?.reload();
                  } finally {
                    setLoadingId(undefined);
                  }
                },
              });
            }}
          >
            审核通过
          </Button>
          <ModalForm
            title="驳回选位"
            trigger={
              <Button type="link" size="small" danger>
                驳回
              </Button>
            }
            modalProps={{ destroyOnClose: true }}
            submitter={{
              submitButtonProps: { loading: rejectLoadingId === record.id },
            }}
            onFinish={async (values) => {
              setRejectLoadingId(record.id);
              try {
                await apiPost<SelectionRow, { reason: string; recommendedBooths?: string }>(
                  `/api/booth-selections/${record.id}/reject`,
                  { reason: values.reason, recommendedBooths: values.recommendedBooths },
                );
                message.success('选位已驳回');
                notifyAdminDashboardStale();
                actionRef.current?.reload();
                return true;
              } finally {
                setRejectLoadingId(undefined);
              }
            }}
          >
            <ProFormTextArea
              name="reason"
              label="驳回原因"
              rules={buildAdminReasonRules('请输入驳回原因')}
              fieldProps={{ showCount: true, maxLength: ADMIN_REASON_MAX_LENGTH }}
            />
            <ProFormTextArea
              name="recommendedBooths"
              label="推荐展位"
              rules={[{ max: ADMIN_REASON_MAX_LENGTH, message: '推荐展位不能超过 500 字' }]}
              fieldProps={{ showCount: true, maxLength: ADMIN_REASON_MAX_LENGTH }}
            />
          </ModalForm>
          <Button
            type="link"
            size="small"
            loading={loadingId === record.id}
            onClick={() => {
              Modal.confirm({
                title: '确认超时释放？',
                content: `选位 ${formatShortId(record.id)} 将释放已锁定展位。`,
                okText: '超时释放',
                cancelText: '取消',
                onOk: async () => {
                  setLoadingId(record.id);
                  try {
                    await apiPost<SelectionRow>(`/api/booth-selections/${record.id}/timeout-release`);
                    message.success('超时释放已完成');
                    notifyAdminDashboardStale();
                    actionRef.current?.reload();
                  } finally {
                    setLoadingId(undefined);
                  }
                },
              });
            }}
          >
            超时释放
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="选位审核">
      <ProTable<SelectionRow>
        actionRef={actionRef}
        rowKey="id"
        search={false}
        columns={columns}
        locale={{ emptyText: '当前没有待审核选位' }}
        request={async () => {
          const selections = await apiGet<BoothSelection[]>('/api/booth-selections?status=PENDING_REVIEW');
          const eventIds = Array.from(new Set(selections.map((selection) => selection.eventId).filter(Boolean)));
          const boothsByEvent = await Promise.all(
            eventIds.map((eventId) => apiGet<Booth[]>(`/api/booths?eventId=${encodeURIComponent(eventId)}`)),
          );
          const boothNoById = buildBoothNoMap(boothsByEvent.flat());
          const data = selections.map((selection) => ({
            ...selection,
            boothNumbersDisplay: formatSelectionBoothNumbers(selection, boothNoById),
          }));
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
