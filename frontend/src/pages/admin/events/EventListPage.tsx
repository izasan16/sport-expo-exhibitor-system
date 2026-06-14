import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType } from '@ant-design/pro-components';
import { Button, message } from 'antd';
import { useRef } from 'react';
import { apiGet, apiPost } from '../../../app/request';

type EventRow = {
  id: string;
  name: string;
  year: number;
  status: string;
};

export default function EventListPage() {
  const actionRef = useRef<ActionType>();

  return (
    <PageContainer title="展会管理">
      <ProTable<EventRow>
        actionRef={actionRef}
        rowKey="id"
        locale={{ emptyText: '先创建展会，再配置展位、面积档位和价格规则' }}
        search={false}
        columns={[
          { title: '展会名称', dataIndex: 'name' },
          { title: '年份', dataIndex: 'year' },
          { title: '状态', dataIndex: 'status' },
        ]}
        request={async () => {
          const data = await apiGet<EventRow[]>('/api/events');
          return { data, success: true, total: data.length };
        }}
        toolBarRender={() => [
          <ModalForm
            key="create"
            title="新建展会"
            trigger={
              <Button type="primary" icon={<PlusOutlined />}>
                新建展会
              </Button>
            }
            modalProps={{ destroyOnClose: true }}
            onFinish={async (values) => {
              await apiPost<EventRow>('/api/events', values);
              message.success('展会已创建');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ProFormText name="name" label="展会名称" rules={[{ required: true, message: '请输入展会名称' }]} />
            <ProFormDigit name="year" label="年份" min={2000} rules={[{ required: true, message: '请输入年份' }]} />
          </ModalForm>,
        ]}
      />
    </PageContainer>
  );
}
