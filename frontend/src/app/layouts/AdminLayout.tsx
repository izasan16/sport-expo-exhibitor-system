import { ProLayout } from '@ant-design/pro-components';
import { Outlet, useNavigate } from 'react-router-dom';

export default function AdminLayout() {
  const navigate = useNavigate();
  return (
    <ProLayout
      title="展商系统后台"
      layout="mix"
      route={{
        path: '/admin',
        routes: [
          { path: '/admin', name: '工作台' },
          { path: '/admin/events', name: '展会管理' },
          { path: '/admin/booths', name: '展位管理' },
        ],
      }}
      menuItemRender={(item, dom) => (
        <a onClick={() => item.path && navigate(item.path)}>{dom}</a>
      )}
    >
      <Outlet />
    </ProLayout>
  );
}
