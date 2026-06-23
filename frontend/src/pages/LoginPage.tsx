import { Button, Card, Form, Input, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../app/AuthProvider';
import { getDefaultPath } from '../app/roleRouting';

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login, ready, user } = useAuth();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!ready || !user) {
      return;
    }
    const returnTo = searchParams.get('returnTo');
    navigate(returnTo && returnTo.startsWith('/') ? returnTo : getDefaultPath(user), { replace: true });
  }, [navigate, ready, searchParams, user]);

  async function handleSubmit(values: { username: string; password: string }) {
    setSubmitting(true);
    try {
      const currentUser = await login(values.username, values.password);
      const returnTo = searchParams.get('returnTo');
      navigate(returnTo && returnTo.startsWith('/') ? returnTo : getDefaultPath(currentUser), { replace: true });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <Card title="中国国际体育用品博览会展商系统" style={{ width: 420 }}>
        <Typography.Paragraph type="secondary">
          请输入账号密码登录对应工作台。
        </Typography.Paragraph>
        <Form layout="vertical" onFinish={handleSubmit} initialValues={{ username: 'admin', password: 'demo123' }}>
          <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting} block>
            登录
          </Button>
        </Form>
      </Card>
    </div>
  );
}
