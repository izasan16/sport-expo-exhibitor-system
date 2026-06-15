import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProFormSelect,
  ProFormText,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, message, Space, Tag } from 'antd';
import { useRef } from 'react';
import { formatExhibitorStatus } from '../../../app/display';
import type { Exhibitor } from '../../../app/domain';
import { apiGet, apiPost, apiPut } from '../../../app/request';

const statusOptions = [
  { label: '正常', value: 'ACTIVE' },
  { label: '已停用', value: 'SUSPENDED' },
  { label: '已归档', value: 'ARCHIVED' },
];

const statusValueEnum = Object.fromEntries(statusOptions.map((option) => [option.value, { text: option.label }]));

function ExhibitorFormFields() {
  return (
    <>
      <ProFormText name="companyName" label="企业名称" rules={[{ required: true, message: '请输入企业名称' }]} />
      <ProFormText name="creditCode" label="统一社会信用代码" />
      <ProFormText name="contactName" label="联系人" />
      <ProFormText name="contactPhone" label="联系电话" />
      <ProFormText name="contactEmail" label="联系邮箱" />
      <ProFormText name="address" label="企业地址" />
      <ProFormSelect
        name="status"
        label="状态"
        options={statusOptions}
        rules={[{ required: true, message: '请选择状态' }]}
      />
    </>
  );
}

export default function AdminExhibitorListPage() {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<Exhibitor>[] = [
    { title: '企业名称', dataIndex: 'companyName', ellipsis: true },
    { title: '统一社会信用代码', dataIndex: 'creditCode', ellipsis: true, hideInSearch: true },
    { title: '联系人', dataIndex: 'contactName', hideInSearch: true },
    { title: '联系电话', dataIndex: 'contactPhone', hideInSearch: true },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      render: (_, record) => <Tag color={record.status === 'ACTIVE' ? 'green' : 'default'}>{formatExhibitorStatus(record.status)}</Tag>,
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={8}>
          <ModalForm
            title="编辑展商"
            trigger={
              <Button type="link" size="small" icon={<EditOutlined />}>
                编辑
              </Button>
            }
            modalProps={{ destroyOnClose: true }}
            initialValues={record}
            onFinish={async (values) => {
              await apiPut<Exhibitor, Partial<Exhibitor>>(`/api/exhibitors/${record.id}`, values);
              message.success('展商信息已更新');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ExhibitorFormFields />
          </ModalForm>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="展商管理">
      <ProTable<Exhibitor>
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        columns={columns}
        request={async () => {
          const data = await apiGet<Exhibitor[]>('/api/exhibitors');
          return { data, success: true, total: data.length };
        }}
        toolBarRender={() => [
          <ModalForm
            key="create"
            title="新建展商"
            trigger={
              <Button type="primary" icon={<PlusOutlined />}>
                新建展商
              </Button>
            }
            modalProps={{ destroyOnClose: true }}
            initialValues={{ status: 'ACTIVE' }}
            onFinish={async (values) => {
              await apiPost<Exhibitor, Partial<Exhibitor>>('/api/exhibitors', values);
              message.success('展商已创建');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ExhibitorFormFields />
          </ModalForm>,
        ]}
      />
    </PageContainer>
  );
}
