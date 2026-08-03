import { ProLayout } from '@ant-design/pro-components';
import { Button, Select, Space, Typography } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthProvider';
import { buildAdminMenu } from '../menuRegistry';
import { getWorkspacePath } from '../roleRouting';
import { ROLE_LABELS } from '../types/rbac';

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { activeRole, permissions, switchRole, user, logout } = useAuth();
  const menuItems = buildAdminMenu(permissions);

  return (
    <ProLayout
      title="展商系统后台"
      layout="mix"
      route={{
        path: '/admin',
        routes: menuItems.map((item) => ({
          path: item.path,
          name: item.label,
          icon: item.icon,
        })),
      }}
      location={{ pathname: location.pathname }}
      menuItemRender={(item, dom) => (
        <a onClick={() => item.path && !item.routes && navigate(item.path)}>{dom}</a>
      )}
      actionsRender={() => [
        <Space key="auth-actions" size="middle">
          {user ? <Typography.Text>{user.displayName}</Typography.Text> : null}
          {user && user.activeAvailableRoles.length > 1 ? (
            <Select
              value={activeRole ?? undefined}
              variant="borderless"
              style={{ minWidth: 140 }}
              options={user.activeAvailableRoles.map((role) => ({
                label: ROLE_LABELS[role],
                value: role,
              }))}
              onChange={(value) => {
                switchRole(value);
                navigate(getWorkspacePath(value));
              }}
            />
          ) : null}
          <Button onClick={() => void logout().then(() => navigate('/login'))}>
            退出
          </Button>
        </Space>,
      ]}
    >
      <Outlet />
    </ProLayout>
  );
}
