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
          { path: '/admin/dashboard', name: '工作台' },
          {
            path: '/admin/business',
            name: '业务处理',
            routes: [
              { path: '/admin/orders', name: '订单管理' },
              { path: '/admin/contracts', name: '合同管理' },
              { path: '/admin/payments', name: '付款确认' },
              { path: '/admin/selections', name: '选位审核' },
            ],
          },
          {
            path: '/admin/config',
            name: '资源配置',
            routes: [
              { path: '/admin/events', name: '展会管理' },
              { path: '/admin/booths', name: '展位管理' },
              { path: '/admin/area-tiers', name: '面积档位' },
              { path: '/admin/price-rules', name: '价格规则' },
            ],
          },
        ],
      }}
      menuItemRender={(item, dom) => (
        <a onClick={() => item.path && !item.routes && navigate(item.path)}>{dom}</a>
      )}
    >
      <Outlet />
    </ProLayout>
  );
}
