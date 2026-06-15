import { BellOutlined, FormOutlined, HomeOutlined, LeftOutlined, OrderedListOutlined } from '@ant-design/icons';
import { Badge, Button, Space, Typography } from 'antd';
import type { ReactNode } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

type ExhibitorNavigationItem = {
  key: string;
  label: string;
  icon: ReactNode;
};

export function getExhibitorNavigationItems(): ExhibitorNavigationItem[] {
  return [
    { key: '/exhibitor', label: '首页', icon: <HomeOutlined /> },
    { key: '/exhibitor/registrations', label: '报名', icon: <FormOutlined /> },
    { key: '/exhibitor/orders', label: '订单', icon: <OrderedListOutlined /> },
    { key: '/exhibitor/notifications', label: '通知', icon: <BellOutlined /> },
  ];
}

export function getExhibitorSelectedNavKey(pathname: string) {
  return getExhibitorNavigationItems()
    .map((item) => item.key)
    .filter((key) => pathname === key || pathname.startsWith(`${key}/`))
    .sort((left, right) => right.length - left.length)[0] ?? '/exhibitor';
}

export function getExhibitorShellClassNames() {
  return {
    root: 'exhibitor-mobile-shell',
    content: 'exhibitor-mobile-content',
    bottomNav: 'exhibitor-bottom-nav',
  };
}

export type ExhibitorHeaderConfig = {
  title: string;
  subtitle: string;
  backTo?: string;
};

export function getExhibitorHeaderConfig(pathname: string): ExhibitorHeaderConfig {
  const orderActionMatch = pathname.match(/^\/exhibitor\/orders\/([^/]+)\/(payment|selection)$/);
  if (orderActionMatch) {
    return {
      title: orderActionMatch[2] === 'payment' ? '提交付款' : '选择展位',
      subtitle: '展商端',
      backTo: `/exhibitor/orders/${orderActionMatch[1]}`,
    };
  }

  if (pathname === '/exhibitor/orders/new') {
    return {
      title: '我要参展',
      subtitle: '展商端',
      backTo: '/exhibitor',
    };
  }

  if (/^\/exhibitor\/orders\/[^/]+$/.test(pathname)) {
    return {
      title: '订单详情',
      subtitle: '展商端',
      backTo: '/exhibitor/orders',
    };
  }

  if (pathname === '/exhibitor/registrations/new') {
    return {
      title: '提交报名',
      subtitle: '展商端',
      backTo: '/exhibitor/registrations',
    };
  }

  if (/^\/exhibitor\/registrations\/[^/]+$/.test(pathname)) {
    return {
      title: '报名详情',
      subtitle: '展商端',
      backTo: '/exhibitor/registrations',
    };
  }

  if (pathname === '/exhibitor/registrations') {
    return { title: '报名进度', subtitle: '展商端' };
  }

  if (pathname === '/exhibitor/profile') {
    return {
      title: '企业资料',
      subtitle: '展商端',
      backTo: '/exhibitor',
    };
  }

  if (pathname === '/exhibitor/orders') {
    return { title: '我的订单', subtitle: '展商端' };
  }

  if (pathname === '/exhibitor/notifications') {
    return { title: '通知中心', subtitle: '展商端' };
  }

  return { title: '参展服务', subtitle: '展商端' };
}

export default function ExhibitorLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const menuItems = getExhibitorNavigationItems();
  const selectedKey = getExhibitorSelectedNavKey(location.pathname);
  const classNames = getExhibitorShellClassNames();
  const header = getExhibitorHeaderConfig(location.pathname);

  return (
    <div className={classNames.root}>
      <header className="exhibitor-app-header">
        <Space size={10} align="center" className="exhibitor-app-header-main">
          {header.backTo ? (
            <Button
              type="text"
              shape="circle"
              aria-label="返回"
              icon={<LeftOutlined />}
              onClick={() => navigate(header.backTo!)}
            />
          ) : null}
          <div>
            <Typography.Text type="secondary">{header.subtitle}</Typography.Text>
            <Typography.Title level={4} style={{ margin: 0 }}>{header.title}</Typography.Title>
          </div>
        </Space>
        <Badge dot>
          <Button shape="circle" icon={<BellOutlined />} onClick={() => navigate('/exhibitor/notifications')} />
        </Badge>
      </header>
      <main className={classNames.content}>
        <Outlet />
      </main>
      <nav className={classNames.bottomNav} aria-label="展商端导航">
        {menuItems.map((item) => {
          const active = item.key === selectedKey;
          return (
            <button
              key={item.key}
              type="button"
              className={active ? 'exhibitor-bottom-nav-item active' : 'exhibitor-bottom-nav-item'}
              onClick={() => navigate(item.key)}
            >
              <span className="exhibitor-bottom-nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
}
