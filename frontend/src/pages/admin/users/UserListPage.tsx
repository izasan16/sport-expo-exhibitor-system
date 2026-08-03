import {
  ModalForm,
  PageContainer,
  ProFormDependency,
  ProFormSelect,
  ProFormText,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import {
  Button,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import AccessGuard from '../../../app/AccessGuard';
import type { Exhibitor } from '../../../app/domain';
import { formatShortId } from '../../../app/formatters';
import { ACTION_PERMISSIONS } from '../../../app/permissionRegistry';
import { apiGet, apiPost, apiPut } from '../../../app/request';
import {
  ROLE_LABELS,
  type CreateUserPayload,
  type RoleSummary,
  type UpdateUserPayload,
  type UpdateUserRolesPayload,
  USER_STATUS_LABELS,
  USER_TYPE_LABELS,
  type UserDetail,
  type UserSummary,
} from '../../../app/types/rbac';
import { formatNullableAdminText, formatRoleLabels } from '../pageLogic';

const USER_TYPE_OPTIONS = [
  { label: USER_TYPE_LABELS.INTERNAL, value: 'INTERNAL' },
  { label: USER_TYPE_LABELS.EXHIBITOR, value: 'EXHIBITOR' },
];

const USER_STATUS_OPTIONS = [
  { label: USER_STATUS_LABELS.ACTIVE, value: 'ACTIVE' },
  { label: USER_STATUS_LABELS.DISABLED, value: 'DISABLED' },
];

function buildUserQuery(params: Record<string, unknown>) {
  const searchParams = new URLSearchParams();
  ['username', 'status', 'roleCode'].forEach((key) => {
    const value = params[key];
    if (value) {
      searchParams.set(key, String(value));
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

export default function UserListPage() {
  const actionRef = useRef<ActionType>();
  const [roles, setRoles] = useState<RoleSummary[]>([]);
  const [exhibitors, setExhibitors] = useState<Exhibitor[]>([]);
  const [detailRecord, setDetailRecord] = useState<UserDetail | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<UserSummary | null>(null);
  const [roleEditingRecord, setRoleEditingRecord] = useState<UserSummary | null>(null);
  const [defaultRoleRecord, setDefaultRoleRecord] = useState<UserSummary | null>(null);
  const [resetPasswordRecord, setResetPasswordRecord] = useState<UserSummary | null>(null);

  useEffect(() => {
    void Promise.all([
      apiGet<RoleSummary[]>('/api/roles'),
      apiGet<Exhibitor[]>('/api/exhibitors'),
    ]).then(([roleRows, exhibitorRows]) => {
      setRoles(roleRows);
      setExhibitors(exhibitorRows);
    });
  }, []);

  const roleOptions = useMemo(
    () => roles.map((role) => ({ label: `${role.name} (${role.code})`, value: role.code })),
    [roles],
  );
  const exhibitorOptions = useMemo(
    () => exhibitors.map((exhibitor) => ({ label: exhibitor.companyName, value: exhibitor.id })),
    [exhibitors],
  );
  const exhibitorNameById = useMemo(
    () => new Map(exhibitors.map((exhibitor) => [exhibitor.id, exhibitor.companyName])),
    [exhibitors],
  );

  const columns: ProColumns<UserSummary>[] = [
    { title: '账号', dataIndex: 'username', ellipsis: true },
    { title: '姓名', dataIndex: 'displayName' },
    {
      title: '用户类型',
      dataIndex: 'userType',
      valueType: 'select',
      valueEnum: Object.fromEntries(USER_TYPE_OPTIONS.map((item) => [item.value, { text: item.label }])),
      render: (_, record) => USER_TYPE_LABELS[record.userType] ?? record.userType,
    },
    {
      title: '绑定展商',
      dataIndex: 'exhibitorId',
      hideInSearch: true,
      render: (_, record) => record.exhibitorId
        ? exhibitorNameById.get(record.exhibitorId) ?? formatShortId(record.exhibitorId)
        : '未绑定',
    },
    {
      title: '角色',
      dataIndex: 'roleCode',
      valueType: 'select',
      valueEnum: Object.fromEntries(roleOptions.map((item) => [item.value, { text: item.label }])),
      render: (_, record) => formatRoleLabels(record.roleCodes),
    },
    {
      title: '默认角色',
      dataIndex: 'defaultRole',
      hideInSearch: true,
      render: (_, record) => record.defaultRole ? (ROLE_LABELS[record.defaultRole as keyof typeof ROLE_LABELS] ?? record.defaultRole) : '未设置',
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: Object.fromEntries(USER_STATUS_OPTIONS.map((item) => [item.value, { text: item.label }])),
      render: (_, record) => (
        <Tag color={record.status === 'ACTIVE' ? 'success' : 'default'}>
          {USER_STATUS_LABELS[record.status] ?? record.status}
        </Tag>
      ),
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={4} wrap>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              const detail = await apiGet<UserDetail>(`/api/users/${record.id}`);
              setDetailRecord(detail);
              setDetailOpen(true);
            }}
          >
            查看
          </Button>
          <AccessGuard permission={ACTION_PERMISSIONS.editUser}>
            <Button type="link" size="small" onClick={() => setEditingRecord(record)}>
              编辑
            </Button>
          </AccessGuard>
          <AccessGuard permission={record.status === 'ACTIVE' ? ACTION_PERMISSIONS.disableUser : ACTION_PERMISSIONS.enableUser}>
            <Button
              type="link"
              size="small"
              onClick={() => {
                Modal.confirm({
                  title: record.status === 'ACTIVE' ? '确认停用该用户？' : '确认启用该用户？',
                  content: `${record.displayName}（${record.username}）将被${record.status === 'ACTIVE' ? '停用' : '启用'}。`,
                  okText: '确认',
                  cancelText: '取消',
                  onOk: async () => {
                    await apiPut<UserDetail, { enabled: boolean }>(`/api/users/${record.id}/status`, {
                      enabled: record.status !== 'ACTIVE',
                    });
                    message.success('用户状态已更新');
                    actionRef.current?.reload();
                  },
                });
              }}
            >
              {record.status === 'ACTIVE' ? '停用' : '启用'}
            </Button>
          </AccessGuard>
          <AccessGuard permission={ACTION_PERMISSIONS.assignUserRoles}>
            <Button type="link" size="small" onClick={() => setRoleEditingRecord(record)}>
              分配角色
            </Button>
          </AccessGuard>
          <AccessGuard permission={ACTION_PERMISSIONS.setDefaultRole}>
            <Button type="link" size="small" onClick={() => setDefaultRoleRecord(record)}>
              默认角色
            </Button>
          </AccessGuard>
          <AccessGuard permission={ACTION_PERMISSIONS.resetPassword}>
            <Button type="link" size="small" onClick={() => setResetPasswordRecord(record)}>
              重置密码
            </Button>
          </AccessGuard>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="用户管理">
      <ProTable<UserSummary>
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 88 }}
        columns={columns}
        request={async (params) => {
          const data = await apiGet<UserSummary[]>(`/api/users${buildUserQuery(params)}`);
          const filtered = data.filter((item) => {
            const displayName = String(params.displayName ?? '').trim();
            const userType = String(params.userType ?? '').trim();
            if (displayName && !item.displayName.toLowerCase().includes(displayName.toLowerCase())) {
              return false;
            }
            if (userType && item.userType !== userType) {
              return false;
            }
            return true;
          });
          return { data: filtered, success: true, total: filtered.length };
        }}
        toolBarRender={() => [
          <AccessGuard key="create-user" permission={ACTION_PERMISSIONS.createUser}>
            <ModalForm<CreateUserPayload>
              title="新建用户"
              trigger={<Button type="primary">新建用户</Button>}
              modalProps={{ destroyOnHidden: true }}
              initialValues={{ userType: 'INTERNAL', status: 'ACTIVE' }}
              onFinish={async (values) => {
                await apiPost<UserDetail, CreateUserPayload>('/api/users', {
                  ...values,
                  exhibitorId: values.userType === 'EXHIBITOR' ? values.exhibitorId ?? null : null,
                });
                message.success('用户已创建');
                actionRef.current?.reload();
                return true;
              }}
            >
              <ProFormText name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]} />
              <ProFormText.Password name="password" label="初始密码" rules={[{ required: true, min: 6, message: '密码至少 6 位' }]} />
              <ProFormText name="displayName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]} />
              <ProFormText name="phone" label="手机号" />
              <ProFormText name="email" label="邮箱" />
              <ProFormSelect name="userType" label="用户类型" options={USER_TYPE_OPTIONS} rules={[{ required: true }]} />
              <ProFormDependency name={['userType']}>
                {({ userType }) => userType === 'EXHIBITOR' ? (
                  <ProFormSelect
                    name="exhibitorId"
                    label="绑定展商"
                    options={exhibitorOptions}
                    rules={[{ required: true, message: '请选择绑定展商' }]}
                  />
                ) : null}
              </ProFormDependency>
              <ProFormSelect name="status" label="状态" options={USER_STATUS_OPTIONS} rules={[{ required: true }]} />
              <ProFormSelect
                name="roleCodes"
                label="角色"
                mode="multiple"
                options={roleOptions}
                rules={[{ required: true, message: '至少选择一个角色' }]}
              />
              <ProFormDependency name={['roleCodes']}>
                {({ roleCodes }) => (
                  <ProFormSelect
                    name="defaultRole"
                    label="默认角色"
                    options={roleOptions.filter((item) => (roleCodes ?? []).includes(item.value))}
                    rules={[{ required: true, message: '请选择默认角色' }]}
                  />
                )}
              </ProFormDependency>
            </ModalForm>
          </AccessGuard>,
        ]}
      />

      <ModalForm<UpdateUserPayload>
        title="编辑用户"
        open={Boolean(editingRecord)}
        modalProps={{ destroyOnHidden: true, onCancel: () => setEditingRecord(null) }}
        initialValues={editingRecord ? {
          username: editingRecord.username,
          displayName: editingRecord.displayName,
          phone: editingRecord.phone ?? undefined,
          email: editingRecord.email ?? undefined,
          userType: editingRecord.userType,
          exhibitorId: editingRecord.exhibitorId ?? undefined,
        } : undefined}
        onFinish={async (values) => {
          if (!editingRecord) return false;
          await apiPut<UserDetail, UpdateUserPayload>(`/api/users/${editingRecord.id}`, {
            ...values,
            exhibitorId: values.userType === 'EXHIBITOR' ? values.exhibitorId ?? null : null,
          });
          message.success('用户资料已更新');
          setEditingRecord(null);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormText name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]} />
        <ProFormText name="displayName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]} />
        <ProFormText name="phone" label="手机号" />
        <ProFormText name="email" label="邮箱" />
        <ProFormSelect name="userType" label="用户类型" options={USER_TYPE_OPTIONS} rules={[{ required: true }]} />
        <ProFormDependency name={['userType']}>
          {({ userType }) => userType === 'EXHIBITOR' ? (
            <ProFormSelect
              name="exhibitorId"
              label="绑定展商"
              options={exhibitorOptions}
              rules={[{ required: true, message: '请选择绑定展商' }]}
            />
          ) : null}
        </ProFormDependency>
      </ModalForm>

      <ModalForm<UpdateUserRolesPayload>
        title="分配角色"
        open={Boolean(roleEditingRecord)}
        modalProps={{ destroyOnHidden: true, onCancel: () => setRoleEditingRecord(null) }}
        initialValues={roleEditingRecord ? {
          roleCodes: roleEditingRecord.roleCodes,
          defaultRole: roleEditingRecord.defaultRole ?? roleEditingRecord.roleCodes[0],
        } : undefined}
        onFinish={async (values) => {
          if (!roleEditingRecord) return false;
          await apiPut<UserDetail, UpdateUserRolesPayload>(`/api/users/${roleEditingRecord.id}/roles`, values);
          message.success('角色分配已更新');
          setRoleEditingRecord(null);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormSelect
          name="roleCodes"
          label="角色"
          mode="multiple"
          options={roleOptions}
          rules={[{ required: true, message: '至少选择一个角色' }]}
        />
        <ProFormDependency name={['roleCodes']}>
          {({ roleCodes }) => (
            <ProFormSelect
              name="defaultRole"
              label="默认角色"
              options={roleOptions.filter((item) => (roleCodes ?? []).includes(item.value))}
              rules={[{ required: true, message: '请选择默认角色' }]}
            />
          )}
        </ProFormDependency>
      </ModalForm>

      <ModalForm<{ defaultRole: string }>
        title="设置默认角色"
        open={Boolean(defaultRoleRecord)}
        modalProps={{ destroyOnHidden: true, onCancel: () => setDefaultRoleRecord(null) }}
        initialValues={defaultRoleRecord ? { defaultRole: defaultRoleRecord.defaultRole ?? defaultRoleRecord.roleCodes[0] } : undefined}
        onFinish={async (values) => {
          if (!defaultRoleRecord) return false;
          await apiPut<UserDetail, { defaultRole: string }>(`/api/users/${defaultRoleRecord.id}/default-role`, values);
          message.success('默认角色已更新');
          setDefaultRoleRecord(null);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormSelect
          name="defaultRole"
          label="默认角色"
          options={roleOptions.filter((item) => (defaultRoleRecord?.roleCodes ?? []).includes(item.value))}
          rules={[{ required: true, message: '请选择默认角色' }]}
        />
      </ModalForm>

      <Modal
        title="重置密码"
        open={Boolean(resetPasswordRecord)}
        destroyOnHidden
        onCancel={() => setResetPasswordRecord(null)}
        footer={null}
      >
        <Form
          layout="vertical"
          onFinish={async (values: { newPassword: string }) => {
            if (!resetPasswordRecord) return;
            await apiPut<UserDetail, { newPassword: string }>(`/api/users/${resetPasswordRecord.id}/reset-password`, values);
            message.success('密码已重置');
            setResetPasswordRecord(null);
            actionRef.current?.reload();
          }}
        >
          <Typography.Paragraph type="secondary">
            将为 {resetPasswordRecord?.displayName}（{resetPasswordRecord?.username}）设置新密码。
          </Typography.Paragraph>
          <Form.Item name="newPassword" label="新密码" rules={[{ required: true, min: 6, message: '密码至少 6 位' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Space>
            <Button onClick={() => setResetPasswordRecord(null)}>取消</Button>
            <Button type="primary" htmlType="submit">确认重置</Button>
          </Space>
        </Form>
      </Modal>

      <Drawer
        title="用户详情"
        width={520}
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false);
          setDetailRecord(null);
        }}
      >
        {detailRecord ? (
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="账号">{detailRecord.username}</Descriptions.Item>
            <Descriptions.Item label="姓名">{detailRecord.displayName}</Descriptions.Item>
            <Descriptions.Item label="用户类型">{USER_TYPE_LABELS[detailRecord.userType] ?? detailRecord.userType}</Descriptions.Item>
            <Descriptions.Item label="绑定展商">
              {detailRecord.exhibitorId
                ? exhibitorNameById.get(detailRecord.exhibitorId) ?? formatShortId(detailRecord.exhibitorId)
                : '未绑定'}
            </Descriptions.Item>
            <Descriptions.Item label="角色">{formatRoleLabels(detailRecord.roleCodes)}</Descriptions.Item>
            <Descriptions.Item label="默认角色">
              {detailRecord.defaultRole ? (ROLE_LABELS[detailRecord.defaultRole as keyof typeof ROLE_LABELS] ?? detailRecord.defaultRole) : '未设置'}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={detailRecord.status === 'ACTIVE' ? 'success' : 'default'}>
                {USER_STATUS_LABELS[detailRecord.status] ?? detailRecord.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="手机号">{formatNullableAdminText(detailRecord.phone, '未填写')}</Descriptions.Item>
            <Descriptions.Item label="邮箱">{formatNullableAdminText(detailRecord.email, '未填写')}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatNullableAdminText(detailRecord.createdAt, '暂无')}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{formatNullableAdminText(detailRecord.updatedAt, '暂无')}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </Drawer>
    </PageContainer>
  );
}
