import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormSelect, ProFormSwitch, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType } from '@ant-design/pro-components';
import { Button, message } from 'antd';
import { useRef } from 'react';
import { formatEventDisplay } from '../../../app/display';
import { apiGet, apiPost } from '../../../app/request';
import { useEvents } from '../../../app/useEvents';

type AreaTierRow = {
  id: string;
  eventId: string;
  zoneId?: string;
  areaSqm: number;
  enabled: boolean;
};

function removeEmptyValues<T extends Record<string, unknown>>(values: T): T {
  return Object.fromEntries(Object.entries(values).filter(([, value]) => value !== undefined && value !== '')) as T;
}

export default function AreaTierListPage() {
  const actionRef = useRef<ActionType>();
  const { events, eventOptions, eventValueEnum } = useEvents();

  return (
    <PageContainer title="面积档位">
      <ProTable<AreaTierRow>
        actionRef={actionRef}
        rowKey="id"
        locale={{ emptyText: '请选择展会查询；若无结果，请配置展商可购买的标准面积档位' }}
        search={{ labelWidth: 'auto' }}
        columns={[
          {
            title: '展会',
            dataIndex: 'eventId',
            valueType: 'select',
            valueEnum: eventValueEnum,
            render: (_, record) => formatEventDisplay(record.eventId, events),
          },
          { title: '分区', dataIndex: 'zoneId', hideInSearch: true, renderText: (value) => value || '不限分区' },
          { title: '面积', dataIndex: 'areaSqm', valueType: 'digit', hideInSearch: true },
          { title: '启用', dataIndex: 'enabled', valueType: 'switch', hideInSearch: true },
        ]}
        request={async (params) => {
          if (!params.eventId) {
            return { data: [], success: true, total: 0 };
          }

          const eventId = encodeURIComponent(String(params.eventId));
          const data = await apiGet<AreaTierRow[]>(`/api/area-tiers?eventId=${eventId}`);
          return { data, success: true, total: data.length };
        }}
        toolBarRender={() => [
          <ModalForm
            key="create"
            title="新建面积档位"
            trigger={
              <Button type="primary" icon={<PlusOutlined />}>
                新建面积档位
              </Button>
            }
            initialValues={{ enabled: true }}
            modalProps={{ destroyOnClose: true }}
            onFinish={async (values) => {
              await apiPost<AreaTierRow>('/api/area-tiers', removeEmptyValues(values));
              message.success('面积档位已创建');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ProFormSelect name="eventId" label="展会" options={eventOptions} rules={[{ required: true, message: '请选择展会' }]} />
            <ProFormText name="zoneId" label="分区（可选）" placeholder="不限定分区可留空" />
            <ProFormDigit name="areaSqm" label="面积" min={0.01} rules={[{ required: true, message: '请输入面积' }]} />
            <ProFormSwitch name="enabled" label="启用" />
          </ModalForm>,
        ]}
      />
    </PageContainer>
  );
}
