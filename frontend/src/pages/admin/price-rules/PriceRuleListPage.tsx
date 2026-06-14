import { PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProFormDigit,
  ProFormSelect,
  ProFormSwitch,
  ProFormText,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType } from '@ant-design/pro-components';
import { Button, message } from 'antd';
import { useRef } from 'react';
import { apiGet, apiPost } from '../../../app/request';

type PriceRuleRow = {
  id: string;
  eventId: string;
  zoneId?: string;
  boothType: string;
  unitPrice: number;
  enabled: boolean;
};

function removeEmptyValues<T extends Record<string, unknown>>(values: T): T {
  return Object.fromEntries(Object.entries(values).filter(([, value]) => value !== undefined && value !== '')) as T;
}

export default function PriceRuleListPage() {
  const actionRef = useRef<ActionType>();

  return (
    <PageContainer title="价格规则">
      <ProTable<PriceRuleRow>
        actionRef={actionRef}
        rowKey="id"
        locale={{ emptyText: '请输入展会 ID 查询；若无结果，请配置不同展位类型的每平方米价格' }}
        search={{ labelWidth: 'auto' }}
        columns={[
          { title: '展会 ID', dataIndex: 'eventId', hideInTable: true },
          { title: '分区 ID', dataIndex: 'zoneId', hideInSearch: true },
          { title: '展位类型', dataIndex: 'boothType', hideInSearch: true },
          { title: '单价', dataIndex: 'unitPrice', valueType: 'money', hideInSearch: true },
          { title: '启用', dataIndex: 'enabled', valueType: 'switch', hideInSearch: true },
        ]}
        request={async (params) => {
          if (!params.eventId) {
            return { data: [], success: true, total: 0 };
          }

          const eventId = encodeURIComponent(String(params.eventId));
          const data = await apiGet<PriceRuleRow[]>(`/api/price-rules?eventId=${eventId}`);
          return { data, success: true, total: data.length };
        }}
        toolBarRender={() => [
          <ModalForm
            key="create"
            title="新建价格规则"
            trigger={
              <Button type="primary" icon={<PlusOutlined />}>
                新建价格规则
              </Button>
            }
            initialValues={{ enabled: true }}
            modalProps={{ destroyOnClose: true }}
            onFinish={async (values) => {
              await apiPost<PriceRuleRow>('/api/price-rules', removeEmptyValues(values));
              message.success('价格规则已创建');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ProFormText name="eventId" label="展会 ID" rules={[{ required: true, message: '请输入展会 ID' }]} />
            <ProFormText name="zoneId" label="分区 ID" />
            <ProFormSelect
              name="boothType"
              label="展位类型"
              options={[
                { label: 'STANDARD', value: 'STANDARD' },
                { label: 'CUSTOM', value: 'CUSTOM' },
              ]}
              rules={[{ required: true, message: '请选择展位类型' }]}
            />
            <ProFormDigit name="unitPrice" label="单价" min={0.01} rules={[{ required: true, message: '请输入单价' }]} />
            <ProFormSwitch name="enabled" label="启用" />
          </ModalForm>,
        ]}
      />
    </PageContainer>
  );
}
