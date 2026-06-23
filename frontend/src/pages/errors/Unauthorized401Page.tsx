import { Button, Card, Space, Typography } from 'antd';
import { useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

export default function Unauthorized401Page() {
  const navigate = useNavigate();
  const location = useLocation();

  const returnTo = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('returnTo');
  }, [location.search]);

  return (
    <div className="login-page">
      <Card style={{ width: 420 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ marginBottom: 0 }}>
            登录已失效
          </Typography.Title>
          <Typography.Text type="secondary">
            请重新登录后继续刚才的操作。
          </Typography.Text>
          <Button
            type="primary"
            block
            onClick={() => navigate(returnTo ? `/login?returnTo=${encodeURIComponent(returnTo)}` : '/login')}
          >
            重新登录
          </Button>
        </Space>
      </Card>
    </div>
  );
}
