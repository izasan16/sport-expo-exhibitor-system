import { Layout, Menu } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';

export default function ExhibitorLayout() {
  const navigate = useNavigate();
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={['home']}
          items={[{ key: 'home', label: '展商中心' }]}
          onClick={() => navigate('/exhibitor')}
        />
      </Layout.Header>
      <Layout.Content style={{ padding: 24 }}>
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}
