import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormSelect, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType } from '@ant-design/pro-components';
import { Button, message } from 'antd';
import { useRef } from 'react';
import { apiGet, apiPost } from '../../../app/request';

type BoothRow = {
  id: string;
  eventId: string;
  boothNo: string;
  areaSqm: number;
  boothType: string;
  status: string;
};

export default function BoothListPage() {
  const actionRef = useRef<ActionType>();

  return (
    <PageContainer title="展位管理">
      <ProTable<BoothRow>
        actionRef={actionRef}
        rowKey="id"
        locale={{ emptyText: '请选择展会后查看展位' }}
        search={{ labelWidth: 'auto' }}
        columns={[
          { title: '展会 ID', dataIndex: 'eventId', hideInTable: true },
          { title: '展位号', dataIndex: 'boothNo' },
          { title: '面积', dataIndex: 'areaSqm', valueType: 'digit' },
          { title: '类型', dataIndex: 'boothType' },
          { title: '状态', dataIndex: 'status' },
        ]}
        request={async (params) => {
          if (!params.eventId) {
            return { data: [], success: true, total: 0 };
          }

          const eventId = encodeURIComponent(String(params.eventId));
          const data = await apiGet<BoothRow[]>(`/api/booths?eventId=${eventId}`);
          return { data, success: true, total: data.length };
        }}
        toolBarRender={() => [
          <ModalForm
            key="create"
            title="新建展位"
            trigger={
              <Button type="primary" icon={<PlusOutlined />}>
                新建展位
              </Button>
            }
            modalProps={{ destroyOnClose: true }}
            onFinish={async (values) => {
              await apiPost<BoothRow>('/api/booths', values);
              message.success('展位已创建');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ProFormText name="eventId" label="展会 ID" rules={[{ required: true, message: '请输入展会 ID' }]} />
            <ProFormText name="boothNo" label="展位号" rules={[{ required: true, message: '请输入展位号' }]} />
            <ProFormDigit name="areaSqm" label="面积" min={0.01} rules={[{ required: true, message: '请输入面积' }]} />
            <ProFormSelect
              name="boothType"
              label="展位类型"
              options={[
                { label: 'STANDARD', value: 'STANDARD' },
                { label: 'CUSTOM', value: 'CUSTOM' },
              ]}
              rules={[{ required: true, message: '请选择展位类型' }]}
            />
          </ModalForm>,
        ]}
      />
    </PageContainer>
  );
}
