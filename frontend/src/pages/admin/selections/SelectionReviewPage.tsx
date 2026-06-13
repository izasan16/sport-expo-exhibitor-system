import { ModalForm, PageContainer, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Space } from 'antd';
import { useRef } from 'react';
import { apiGet, apiPost } from '../../../app/request';

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

  const columns: ProColumns<SelectionRow>[] = [
    { title: '订单 ID', dataIndex: 'orderId', ellipsis: true },
    { title: '展商 ID', dataIndex: 'exhibitorId', ellipsis: true },
    { title: '状态', dataIndex: 'status' },
    { title: '总面积', dataIndex: 'totalAreaSqm', valueType: 'digit' },
    { title: '展位数', dataIndex: 'boothIds', renderText: (boothIds: string[]) => boothIds?.length ?? 0 },
    { title: '锁定到期', dataIndex: 'lockExpiresAt', valueType: 'dateTime' },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await apiPost<SelectionRow>(`/api/booth-selections/${record.id}/approve`);
              message.success('选位审核已通过');
              actionRef.current?.reload();
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
            onFinish={async (values) => {
              await apiPost<SelectionRow>(`/api/booth-selections/${record.id}/reject`, values);
              message.success('选位已驳回');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ProFormText name="reason" label="驳回原因" rules={[{ required: true, message: '请输入驳回原因' }]} />
          </ModalForm>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await apiPost<SelectionRow>(`/api/booth-selections/${record.id}/timeout-release`);
              message.success('超时释放已完成');
              actionRef.current?.reload();
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
        request={async () => {
          const data = await apiGet<SelectionRow[]>('/api/booth-selections?status=PENDING_REVIEW');
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
