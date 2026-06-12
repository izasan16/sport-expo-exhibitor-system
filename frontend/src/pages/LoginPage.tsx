import { Button, Card, Space, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function LoginPage() {
  const navigate = useNavigate();
  return (
    <div className="login-page">
      <Card title="中国国际体育用品博览会展商系统" style={{ width: 420 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Text type="secondary">请选择入口</Typography.Text>
          <Button type="primary" block onClick={() => navigate('/admin')}>运营后台</Button>
          <Button block onClick={() => navigate('/exhibitor')}>展商中心</Button>
        </Space>
      </Card>
    </div>
  );
}
