import { Layout, Menu } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

export default function ExhibitorLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const menuItems = [
    { key: '/exhibitor', label: '首页' },
    { key: '/exhibitor/orders/new', label: '我要参展' },
    { key: '/exhibitor/orders', label: '我的订单' },
    { key: '/exhibitor/notifications', label: '通知中心' },
  ];
  const selectedKey =
    menuItems
      .map((item) => item.key)
      .filter((key) => location.pathname === key || location.pathname.startsWith(`${key}/`))
      .sort((left, right) => right.length - left.length)[0] ?? '/exhibitor';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Layout.Header>
      <Layout.Content style={{ padding: 24 }}>
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}
