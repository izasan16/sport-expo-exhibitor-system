import { PageContainer } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  List,
  Modal,
  Row,
  Segmented,
  Space,
  Tag,
  Tree,
  Typography,
  message,
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import AccessGuard from '../../../app/AccessGuard';
import { ACTION_PERMISSIONS } from '../../../app/permissionRegistry';
import { apiGet, apiPut } from '../../../app/request';
import {
  PERMISSION_RESOURCE_TYPE_LABELS,
  ROLE_TYPE_LABELS,
  type PermissionTreeNode,
  type RoleDetail,
  type RoleSummary,
} from '../../../app/types/rbac';
import {
  buildCheckedPermissionKeys,
  buildPermissionLabelMap,
  diffPermissionSelection,
  filterPermissionTreeByType,
  toPermissionTreeData,
} from '../rbac/rbacViewModels';

export default function RolePermissionMatrixPage() {
  const [searchParams] = useSearchParams();
  const [roles, setRoles] = useState<RoleSummary[]>([]);
  const [permissionTree, setPermissionTree] = useState<PermissionTreeNode[]>([]);
  const [selectedRole, setSelectedRole] = useState<RoleSummary | null>(null);
  const [selectedRoleDetail, setSelectedRoleDetail] = useState<RoleDetail | null>(null);
  const [checkedKeys, setCheckedKeys] = useState<string[]>([]);
  const [resourceType, setResourceType] = useState<'ALL' | 'MENU' | 'PAGE' | 'ACTION'>('ALL');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void Promise.all([
      apiGet<RoleSummary[]>('/api/roles'),
      apiGet<PermissionTreeNode[]>('/api/permissions/tree'),
    ]).then(([roleRows, treeRows]) => {
      setRoles(roleRows);
      setPermissionTree(treeRows);
      const queryRoleId = searchParams.get('roleId');
      const nextRole = roleRows.find((item) => item.id === queryRoleId) ?? roleRows[0] ?? null;
      setSelectedRole(nextRole);
    });
  }, [searchParams]);

  useEffect(() => {
    if (!selectedRole) {
      setSelectedRoleDetail(null);
      setCheckedKeys([]);
      return;
    }
    void apiGet<RoleDetail>(`/api/roles/${selectedRole.id}/permissions`).then((detail) => {
      setSelectedRoleDetail(detail);
      setCheckedKeys(buildCheckedPermissionKeys(detail.permissionCodes));
    });
  }, [selectedRole]);

  const filteredTree = useMemo(
    () => filterPermissionTreeByType(permissionTree, resourceType),
    [permissionTree, resourceType],
  );
  const permissionLabelMap = useMemo(() => buildPermissionLabelMap(permissionTree), [permissionTree]);
  const pendingChanges = useMemo(() => diffPermissionSelection(
    selectedRoleDetail?.permissionCodes ?? [],
    checkedKeys,
    permissionLabelMap,
  ), [checkedKeys, permissionLabelMap, selectedRoleDetail?.permissionCodes]);

  const hasChanges = pendingChanges.added.length > 0 || pendingChanges.removed.length > 0;

  return (
    <PageContainer title="授权矩阵">
      <Row gutter={16} align="top">
        <Col xs={24} lg={7}>
          <Card title="角色列表">
            <List
              dataSource={roles}
              locale={{ emptyText: <Empty description="暂无角色" /> }}
              renderItem={(role) => (
                <List.Item
                  style={{
                    cursor: 'pointer',
                    borderRadius: 12,
                    paddingInline: 12,
                    background: selectedRole?.id === role.id ? '#f0f7ff' : undefined,
                  }}
                  onClick={() => setSelectedRole(role)}
                >
                  <List.Item.Meta
                    title={role.name}
                    description={(
                      <Space wrap size={[6, 6]}>
                        <Tag>{role.code}</Tag>
                        <Tag>{ROLE_TYPE_LABELS[role.roleType] ?? role.roleType}</Tag>
                        <Tag color={role.enabled ? 'success' : 'default'}>
                          {role.enabled ? '启用' : '停用'}
                        </Tag>
                      </Space>
                    )}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} lg={17}>
          <Card
            title={selectedRole ? `${selectedRole.name} 权限配置` : '权限配置'}
            extra={(
              <Segmented
                options={[
                  { label: '全部', value: 'ALL' },
                  { label: '菜单', value: 'MENU' },
                  { label: '页面', value: 'PAGE' },
                  { label: '操作', value: 'ACTION' },
                ]}
                value={resourceType}
                onChange={(value) => setResourceType(value as typeof resourceType)}
              />
            )}
          >
            {selectedRoleDetail ? (
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                <Alert
                  type="info"
                  showIcon
                  message="当前角色说明"
                  description={selectedRoleDetail.description || '暂无说明，可直接在角色管理页补充。'}
                />
                <Space wrap>
                  <Tag>{selectedRoleDetail.code}</Tag>
                  <Tag>{ROLE_TYPE_LABELS[selectedRoleDetail.roleType] ?? selectedRoleDetail.roleType}</Tag>
                  <Tag color={selectedRoleDetail.enabled ? 'success' : 'default'}>
                    {selectedRoleDetail.enabled ? '启用' : '停用'}
                  </Tag>
                  <Tag color="processing">{checkedKeys.length} 项已选</Tag>
                </Space>
                <Tree
                  checkable
                  defaultExpandAll
                  checkedKeys={checkedKeys}
                  treeData={toPermissionTreeData(filteredTree)}
                  onCheck={(keys) => setCheckedKeys((Array.isArray(keys) ? keys : keys.checked).map(String))}
                />
                {hasChanges ? (
                  <Alert
                    type="warning"
                    showIcon
                    message="待保存变更"
                    description={(
                      <Space direction="vertical" size={6} style={{ width: '100%' }}>
                        <Typography.Text>
                          新增：{pendingChanges.added.length > 0 ? pendingChanges.added.join('、') : '无'}
                        </Typography.Text>
                        <Typography.Text>
                          移除：{pendingChanges.removed.length > 0 ? pendingChanges.removed.join('、') : '无'}
                        </Typography.Text>
                      </Space>
                    )}
                  />
                ) : null}
                <AccessGuard permission={ACTION_PERMISSIONS.assignRolePermissions}>
                  <Button
                    type="primary"
                    loading={saving}
                    disabled={!selectedRole || !hasChanges}
                    onClick={() => {
                      if (!selectedRole) {
                        return;
                      }
                      Modal.confirm({
                        title: '确认保存角色权限？',
                        content: (
                          <Space direction="vertical" size={8}>
                            <Typography.Text>
                              新增：{pendingChanges.added.length > 0 ? pendingChanges.added.join('、') : '无'}
                            </Typography.Text>
                            <Typography.Text>
                              移除：{pendingChanges.removed.length > 0 ? pendingChanges.removed.join('、') : '无'}
                            </Typography.Text>
                          </Space>
                        ),
                        onOk: async () => {
                          setSaving(true);
                          try {
                            const detail = await apiPut<RoleDetail, { permissionCodes: string[] }>(
                              `/api/roles/${selectedRole.id}/permissions`,
                              { permissionCodes: checkedKeys },
                            );
                            setSelectedRoleDetail(detail);
                            setCheckedKeys(buildCheckedPermissionKeys(detail.permissionCodes));
                            message.success('角色权限已保存');
                          } finally {
                            setSaving(false);
                          }
                        },
                      });
                    }}
                  >
                    保存权限配置
                  </Button>
                </AccessGuard>
              </Space>
            ) : (
              <Empty description="请选择左侧角色后配置权限" />
            )}
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
}
