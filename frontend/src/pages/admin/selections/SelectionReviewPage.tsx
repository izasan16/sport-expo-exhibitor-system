import { ModalForm, PageContainer, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Space } from 'antd';
import { useRef, useState } from 'react';
import { formatArea, formatFriendlyTime, formatShortId } from '../../../app/formatters';
import { apiGet, apiPost } from '../../../app/request';
import { getSelectionStatusMeta, StatusTag } from '../../../app/status';

type SelectionRow = {
  id: string;
  eventId: string;
  orderId: string;
  exhibitorId: string;
  status: string;
  totalAreaSqm: number;
  lockStartedAt?: string;
  lockExpiresAt?: string;
  reviewerUserId?: string;
  reviewedAt?: string;
  reviewComment?: string;
  boothIds: string[];
  createdAt: string;
  updatedAt: string;
};

export default function SelectionReviewPage() {
  const actionRef = useRef<ActionType>();
  const [loadingId, setLoadingId] = useState<string>();
  const [rejectLoadingId, setRejectLoadingId] = useState<string>();

  const columns: ProColumns<SelectionRow>[] = [
    { title: '订单', dataIndex: 'orderId', ellipsis: true, render: (_, record) => formatShortId(record.orderId) },
    { title: '展商', dataIndex: 'exhibitorId', ellipsis: true, render: (_, record) => formatShortId(record.exhibitorId) },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => <StatusTag meta={getSelectionStatusMeta(record.status)} />,
    },
    { title: '总面积', dataIndex: 'totalAreaSqm', render: (_, record) => formatArea(record.totalAreaSqm) },
    { title: '展位数', dataIndex: 'boothIds', renderText: (boothIds: string[]) => boothIds?.length ?? 0 },
    { title: '锁定到期', dataIndex: 'lockExpiresAt', render: (_, record) => formatFriendlyTime(record.lockExpiresAt) },
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
                await apiPost<SelectionRow>(`/api/booth-selections/${record.id}/reject`, values);
                message.success('选位已驳回');
                actionRef.current?.reload();
                return true;
              } finally {
                setRejectLoadingId(undefined);
              }
            }}
          >
            <ProFormText name="reason" label="驳回原因" rules={[{ required: true, message: '请输入驳回原因' }]} />
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
          const data = await apiGet<SelectionRow[]>('/api/booth-selections?status=PENDING_REVIEW');
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
