import { ModalForm, PageContainer, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Modal, Space, Tag, Typography, message } from 'antd';
import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AccessGuard from '../../../app/AccessGuard';
import { ACTION_PERMISSIONS } from '../../../app/permissionRegistry';
import { apiGet, apiPost, apiPut } from '../../../app/request';
import {
  type CreateRolePayload,
  ROLE_TYPE_LABELS,
  type RoleDetail,
  type RoleSummary,
  type UpdateRolePayload,
} from '../../../app/types/rbac';
import { formatNullableAdminText } from '../pageLogic';

export default function RoleListPage() {
  const actionRef = useRef<ActionType>();
  const navigate = useNavigate();
  const [editingRole, setEditingRole] = useState<RoleSummary | null>(null);
  const [copyingRole, setCopyingRole] = useState<RoleSummary | null>(null);

  const columns: ProColumns<RoleSummary>[] = [
    { title: '角色名称', dataIndex: 'name' },
    { title: '角色编码', dataIndex: 'code', ellipsis: true },
    {
      title: '类型',
      dataIndex: 'roleType',
      render: (_, record) => <Tag>{ROLE_TYPE_LABELS[record.roleType] ?? record.roleType}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      render: (_, record) => (
        <Tag color={record.enabled ? 'success' : 'default'}>
          {record.enabled ? '启用' : '停用'}
        </Tag>
      ),
    },
    {
      title: '说明',
      dataIndex: 'description',
      render: (_, record) => formatNullableAdminText(record.description, '暂无说明'),
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={4} wrap>
          <AccessGuard permission={ACTION_PERMISSIONS.editRole}>
            <Button type="link" size="small" onClick={() => setEditingRole(record)}>
              编辑
            </Button>
          </AccessGuard>
          <AccessGuard permission={ACTION_PERMISSIONS.copyRole}>
            <Button type="link" size="small" onClick={() => setCopyingRole(record)}>
              复制
            </Button>
          </AccessGuard>
          <AccessGuard permission={ACTION_PERMISSIONS.toggleRoleStatus}>
            <Button
              type="link"
              size="small"
              onClick={() => {
                Modal.confirm({
                  title: record.enabled ? '确认停用该角色？' : '确认启用该角色？',
                  content: `${record.name}（${record.code}）将被${record.enabled ? '停用' : '启用'}。`,
                  onOk: async () => {
                    await apiPut<RoleDetail, { enabled: boolean }>(`/api/roles/${record.id}/status`, {
                      enabled: !record.enabled,
                    });
                    message.success('角色状态已更新');
                    actionRef.current?.reload();
                  },
                });
              }}
            >
              {record.enabled ? '停用' : '启用'}
            </Button>
          </AccessGuard>
          <AccessGuard permission={ACTION_PERMISSIONS.assignRolePermissions}>
            <Button
              type="link"
              size="small"
              onClick={() => navigate(`/admin/role-permissions?roleId=${record.id}`)}
            >
              配置权限
            </Button>
          </AccessGuard>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="角色管理">
      <Typography.Paragraph type="secondary">
        系统预置角色可调权限和启停，自定义角色可继续扩展，本轮暂不提供物理删除。
      </Typography.Paragraph>
      <ProTable<RoleSummary>
        actionRef={actionRef}
        rowKey="id"
        search={false}
        columns={columns}
        request={async () => {
          const data = await apiGet<RoleSummary[]>('/api/roles');
          return { data, success: true, total: data.length };
        }}
        toolBarRender={() => [
          <AccessGuard key="create-role" permission={ACTION_PERMISSIONS.createRole}>
            <ModalForm<CreateRolePayload>
              title="新建角色"
              trigger={<Button type="primary">新建角色</Button>}
              modalProps={{ destroyOnHidden: true }}
              onFinish={async (values) => {
                await apiPost<RoleDetail, CreateRolePayload>('/api/roles', values);
                message.success('角色已创建');
                actionRef.current?.reload();
                return true;
              }}
            >
              <ProFormText name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]} />
              <ProFormText name="code" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]} />
              <ProFormText name="description" label="说明" />
            </ModalForm>
          </AccessGuard>,
        ]}
      />

      <ModalForm<UpdateRolePayload>
        title="编辑角色"
        open={Boolean(editingRole)}
        modalProps={{ destroyOnHidden: true, onCancel: () => setEditingRole(null) }}
        initialValues={editingRole ?? undefined}
        onFinish={async (values) => {
          if (!editingRole) return false;
          await apiPut<RoleDetail, UpdateRolePayload>(`/api/roles/${editingRole.id}`, values);
          message.success('角色已更新');
          setEditingRole(null);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormText name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]} />
        <ProFormText name="code" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]} />
        <ProFormText name="description" label="说明" />
      </ModalForm>

      <ModalForm<CreateRolePayload>
        title="复制角色"
        open={Boolean(copyingRole)}
        modalProps={{ destroyOnHidden: true, onCancel: () => setCopyingRole(null) }}
        initialValues={copyingRole ? {
          name: `${copyingRole.name}-副本`,
          code: `${copyingRole.code}_COPY`,
          description: copyingRole.description ?? undefined,
        } : undefined}
        onFinish={async (values) => {
          if (!copyingRole) return false;
          await apiPost<RoleDetail, CreateRolePayload>(`/api/roles/${copyingRole.id}/copy`, values);
          message.success('角色副本已创建');
          setCopyingRole(null);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormText name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]} />
        <ProFormText name="code" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]} />
        <ProFormText name="description" label="说明" />
      </ModalForm>
    </PageContainer>
  );
}
