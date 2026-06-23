import { Button, Card, Space, Tag, Typography } from 'antd';
import { useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/AuthProvider';
import { formatRoleDisplay } from '../../app/display';
import { getWorkspacePath, hasAdminWorkspace, hasExhibitorWorkspace } from '../../app/roleRouting';

export default function Forbidden403Page() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, activeRole, switchRole } = useAuth();

  const requiredPermission = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('requiredPermission') || '';
  }, [location.search]);

  const alternateRole = useMemo(() => {
    if (!user) {
      return null;
    }
    if (activeRole === 'EXHIBITOR' && hasAdminWorkspace(user)) {
      return user.activeAvailableRoles.find((role) => role !== 'EXHIBITOR') ?? null;
    }
    if (activeRole !== 'EXHIBITOR' && hasExhibitorWorkspace(user)) {
      return 'EXHIBITOR';
    }
    return null;
  }, [activeRole, user]);

  return (
    <div className="login-page">
      <Card style={{ width: 460 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ marginBottom: 0 }}>
            无权限访问
          </Typography.Title>
          <Typography.Text type="secondary">
            您当前没有访问此页面的权限，请联系管理员调整角色或授权。
          </Typography.Text>
          {requiredPermission ? (
            <div>
              <Typography.Text strong>需要权限：</Typography.Text>
              <Tag color="processing" style={{ marginInlineStart: 8 }}>
                {requiredPermission}
              </Tag>
            </div>
          ) : null}
          {user ? (
            <div>
              <Typography.Text strong>当前角色：</Typography.Text>
              <Space wrap style={{ marginInlineStart: 8 }}>
                {user.activeAvailableRoles.map((role) => (
                  <Tag key={role} color={role === activeRole ? 'blue' : 'default'}>
                    {formatRoleDisplay(role)}
                  </Tag>
                ))}
              </Space>
            </div>
          ) : null}
          <Space wrap>
            <Button onClick={() => navigate(-1)}>返回上一页</Button>
            {alternateRole ? (
              <Button
                type="primary"
                onClick={() => {
                  switchRole(alternateRole);
                  navigate(getWorkspacePath(alternateRole));
                }}
              >
                切换工作台
              </Button>
            ) : null}
          </Space>
        </Space>
      </Card>
    </div>
  );
}
