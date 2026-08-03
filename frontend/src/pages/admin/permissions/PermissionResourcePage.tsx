import {
  ModalForm,
  PageContainer,
  ProFormDigit,
  ProFormSelect,
  ProFormSwitch,
  ProFormText,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Card, Col, Modal, Row, Segmented, Space, Tree, message } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { DataNode } from 'antd/es/tree';
import AccessGuard from '../../../app/AccessGuard';
import { ACTION_PERMISSIONS } from '../../../app/permissionRegistry';
import { apiGet, apiPost, apiPut } from '../../../app/request';
import {
  PERMISSION_RESOURCE_TYPE_LABELS,
  type PermissionRequestPayload,
  type PermissionResource,
  type PermissionTreeNode,
} from '../../../app/types/rbac';
import {
  buildPermissionLabelMap,
  filterPermissionTreeByType,
  flattenPermissionTree,
} from '../rbac/rbacViewModels';
import { formatNullableAdminText } from '../pageLogic';

const RESOURCE_TYPE_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '菜单', value: 'MENU' },
  { label: '页面', value: 'PAGE' },
  { label: '操作', value: 'ACTION' },
];

function buildTreeData(nodes: PermissionTreeNode[]): DataNode[] {
  return nodes.map((node) => ({
    key: node.id,
    title: `${node.name} (${node.code})`,
    children: buildTreeData(node.children ?? []),
  }));
}

export default function PermissionResourcePage() {
  const actionRef = useRef<ActionType>();
  const [permissionTree, setPermissionTree] = useState<PermissionTreeNode[]>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string>();
  const [resourceTypeFilter, setResourceTypeFilter] = useState<'ALL' | 'MENU' | 'PAGE' | 'ACTION'>('ALL');
  const [editingRecord, setEditingRecord] = useState<PermissionResource | null>(null);

  const loadTree = async () => {
    const tree = await apiGet<PermissionTreeNode[]>('/api/permissions/tree');
    setPermissionTree(tree);
  };

  useEffect(() => {
    void loadTree();
  }, []);

  const filteredTree = useMemo(
    () => filterPermissionTreeByType(permissionTree, resourceTypeFilter),
    [permissionTree, resourceTypeFilter],
  );
  const flatPermissions = useMemo(() => flattenPermissionTree(permissionTree), [permissionTree]);
  const permissionLabelMap = useMemo(() => buildPermissionLabelMap(permissionTree), [permissionTree]);

  const parentOptions = useMemo(
    () => flatPermissions.map((permission) => ({
      label: `${permission.name} (${permission.code})`,
      value: permission.id,
    })),
    [flatPermissions],
  );

  const columns: ProColumns<PermissionResource>[] = [
    { title: '名称', dataIndex: 'name' },
    { title: '编码', dataIndex: 'code', ellipsis: true },
    {
      title: '类型',
      dataIndex: 'resourceType',
      render: (_, record) => PERMISSION_RESOURCE_TYPE_LABELS[record.resourceType] ?? record.resourceType,
    },
    { title: '模块', dataIndex: 'moduleKey' },
    {
      title: '父级',
      dataIndex: 'parentId',
      hideInSearch: true,
      render: (_, record) => record.parentId ? permissionLabelMap[flatPermissions.find((item) => item.id === record.parentId)?.code ?? ''] ?? '已关联' : '顶级资源',
    },
    {
      title: '路径/动作',
      hideInSearch: true,
      render: (_, record) => record.routePath || record.actionKey || '暂无',
    },
    { title: '排序', dataIndex: 'sortOrder', hideInSearch: true },
    {
      title: '状态',
      dataIndex: 'enabled',
      hideInSearch: true,
      render: (_, record) => record.enabled ? '启用' : '停用',
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space size={4} wrap>
          <AccessGuard permission={ACTION_PERMISSIONS.editPermission}>
            <Button type="link" size="small" onClick={() => setEditingRecord(record)}>
              编辑
            </Button>
          </AccessGuard>
          <AccessGuard permission={ACTION_PERMISSIONS.togglePermissionStatus}>
            <Button
              type="link"
              size="small"
              onClick={() => {
                Modal.confirm({
                  title: record.enabled ? '确认停用该权限资源？' : '确认启用该权限资源？',
                  onOk: async () => {
                    await apiPut<PermissionResource>(`/api/permissions/${record.id}/status?enabled=${String(!record.enabled)}`);
                    message.success('权限资源状态已更新');
                    actionRef.current?.reload();
                    void loadTree();
                  },
                });
              }}
            >
              {record.enabled ? '停用' : '启用'}
            </Button>
          </AccessGuard>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="权限资源">
      <Row gutter={16} align="top">
        <Col xs={24} lg={8}>
          <Card
            title="资源树"
            extra={(
              <Segmented
                size="small"
                options={RESOURCE_TYPE_OPTIONS}
                value={resourceTypeFilter}
                onChange={(value) => setResourceTypeFilter(value as typeof resourceTypeFilter)}
              />
            )}
          >
            <Tree
              selectedKeys={selectedNodeId ? [selectedNodeId] : []}
              treeData={buildTreeData(filteredTree)}
              onSelect={(keys) => setSelectedNodeId(keys[0] as string | undefined)}
            />
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <ProTable<PermissionResource>
            actionRef={actionRef}
            rowKey="id"
            search={false}
            columns={columns}
            params={{ selectedNodeId, resourceTypeFilter }}
            request={async () => {
              const data = await apiGet<PermissionResource[]>('/api/permissions');
              const filtered = data.filter((item) => {
                if (resourceTypeFilter !== 'ALL' && item.resourceType !== resourceTypeFilter) {
                  return false;
                }
                if (selectedNodeId && item.id !== selectedNodeId && item.parentId !== selectedNodeId) {
                  return false;
                }
                return true;
              });
              return { data: filtered, success: true, total: filtered.length };
            }}
            toolBarRender={() => [
              <AccessGuard key="create-permission" permission={ACTION_PERMISSIONS.createPermission}>
                <ModalForm<PermissionRequestPayload>
                  title="新建权限资源"
                  trigger={<Button type="primary">新建资源</Button>}
                  modalProps={{ destroyOnHidden: true }}
                  initialValues={{ moduleKey: 'admin', resourceType: 'PAGE', sortOrder: 100, enabled: true }}
                  onFinish={async (values) => {
                    await apiPost<PermissionResource, PermissionRequestPayload>('/api/permissions', values);
                    message.success('权限资源已创建');
                    actionRef.current?.reload();
                    void loadTree();
                    return true;
                  }}
                >
                  <ProFormText name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]} />
                  <ProFormText name="code" label="编码" rules={[{ required: true, message: '请输入编码' }]} />
                  <ProFormSelect
                    name="resourceType"
                    label="类型"
                    options={RESOURCE_TYPE_OPTIONS.filter((item) => item.value !== 'ALL')}
                    rules={[{ required: true, message: '请选择类型' }]}
                  />
                  <ProFormText name="moduleKey" label="模块" rules={[{ required: true, message: '请输入模块标识' }]} />
                  <ProFormSelect name="parentId" label="父级资源" allowClear options={parentOptions} />
                  <ProFormText name="routePath" label="路由路径" />
                  <ProFormText name="actionKey" label="动作标识" />
                  <ProFormDigit name="sortOrder" label="排序" min={0} />
                  <ProFormSwitch name="enabled" label="启用" />
                  <ProFormText name="description" label="说明" />
                </ModalForm>
              </AccessGuard>,
            ]}
          />
        </Col>
      </Row>

      <ModalForm<PermissionRequestPayload>
        title="编辑权限资源"
        open={Boolean(editingRecord)}
        modalProps={{ destroyOnHidden: true, onCancel: () => setEditingRecord(null) }}
        initialValues={editingRecord ? {
          ...editingRecord,
          routePath: editingRecord.routePath ?? undefined,
          actionKey: editingRecord.actionKey ?? undefined,
          parentId: editingRecord.parentId ?? undefined,
          description: editingRecord.description ?? undefined,
        } : undefined}
        onFinish={async (values) => {
          if (!editingRecord) return false;
          await apiPut<PermissionResource, PermissionRequestPayload>(`/api/permissions/${editingRecord.id}`, values);
          message.success('权限资源已更新');
          setEditingRecord(null);
          actionRef.current?.reload();
          void loadTree();
          return true;
        }}
      >
        <ProFormText name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]} />
        <ProFormText name="code" label="编码" rules={[{ required: true, message: '请输入编码' }]} />
        <ProFormSelect
          name="resourceType"
          label="类型"
          options={RESOURCE_TYPE_OPTIONS.filter((item) => item.value !== 'ALL')}
          rules={[{ required: true, message: '请选择类型' }]}
        />
        <ProFormText name="moduleKey" label="模块" rules={[{ required: true, message: '请输入模块标识' }]} />
        <ProFormSelect name="parentId" label="父级资源" allowClear options={parentOptions} />
        <ProFormText name="routePath" label="路由路径" />
        <ProFormText name="actionKey" label="动作标识" />
        <ProFormDigit name="sortOrder" label="排序" min={0} />
        <ProFormSwitch name="enabled" label="启用" />
        <ProFormText name="description" label="说明" />
      </ModalForm>
    </PageContainer>
  );
}
