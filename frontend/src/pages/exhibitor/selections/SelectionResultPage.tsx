import { PageContainer, ProForm, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { apiGet } from '../../../app/request';

type SelectionRow = {
  id: string;
  eventId: string;
  orderId: string;
  exhibitorId: string;
  status: string;
  totalAreaSqm: number | string;
  lockStartedAt?: string;
  lockExpiresAt?: string;
  reviewedAt?: string;
  reviewComment?: string;
  boothIds: string[];
  createdAt: string;
  updatedAt: string;
};

type FilterValues = {
  orderId: string;
};

export default function SelectionResultPage() {
  const [searchParams] = useSearchParams();
  const initialOrderId = searchParams.get('orderId') ?? '';
  const [orderId, setOrderId] = useState(initialOrderId);

  const columns: ProColumns<SelectionRow>[] = [
    { title: '选位 ID', dataIndex: 'id', ellipsis: true },
    { title: '订单 ID', dataIndex: 'orderId', ellipsis: true },
    { title: '状态', dataIndex: 'status' },
    { title: '总面积', dataIndex: 'totalAreaSqm', valueType: 'digit' },
    { title: '展位数', dataIndex: 'boothIds', renderText: (boothIds: string[]) => boothIds?.length ?? 0 },
    { title: '展位 ID', dataIndex: 'boothIds', ellipsis: true, renderText: (boothIds: string[]) => boothIds?.join(', ') },
    { title: '锁定开始', dataIndex: 'lockStartedAt', valueType: 'dateTime' },
    { title: '锁定到期', dataIndex: 'lockExpiresAt', valueType: 'dateTime' },
    { title: '审核时间', dataIndex: 'reviewedAt', valueType: 'dateTime' },
    { title: '审核意见', dataIndex: 'reviewComment', ellipsis: true },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime' },
  ];

  return (
    <PageContainer title="选位结果">
      <ProForm<FilterValues>
        layout="horizontal"
        initialValues={{ orderId: initialOrderId }}
        submitter={{ searchConfig: { submitText: '查询' } }}
        onFinish={async (values) => {
          setOrderId(values.orderId);
          return true;
        }}
      >
        <ProFormText name="orderId" label="订单 ID" rules={[{ required: true, message: '请输入订单 ID' }]} />
      </ProForm>

      <ProTable<SelectionRow>
        rowKey="id"
        search={false}
        columns={columns}
        params={{ orderId }}
        request={async (params) => {
          if (!params.orderId) {
            return { data: [], success: true, total: 0 };
          }
          const data = await apiGet<SelectionRow[]>(
            `/api/booth-selections?orderId=${encodeURIComponent(String(params.orderId))}`,
          );
          return { data, success: true, total: data.length };
        }}
      />
    </PageContainer>
  );
}
