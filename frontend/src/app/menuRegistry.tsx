import {
  ApartmentOutlined,
  AuditOutlined,
  CalendarOutlined,
  ControlOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  HomeOutlined,
  IdcardOutlined,
  KeyOutlined,
  OrderedListOutlined,
  SafetyCertificateOutlined,
  ShopOutlined,
  SolutionOutlined,
  TagsOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { MENU_PERMISSIONS } from './permissionRegistry';
import type { AdminMenuItem } from './types/rbac';

const ADMIN_MENU_REGISTRY: AdminMenuItem[] = [
  { key: '/admin/dashboard', path: '/admin/dashboard', label: '工作台', permission: MENU_PERMISSIONS.adminDashboard, icon: <HomeOutlined /> },
  { key: '/admin/users', path: '/admin/users', label: '用户管理', permission: MENU_PERMISSIONS.adminUsers, icon: <TeamOutlined /> },
  { key: '/admin/roles', path: '/admin/roles', label: '角色管理', permission: MENU_PERMISSIONS.adminRoles, icon: <IdcardOutlined /> },
  { key: '/admin/permissions', path: '/admin/permissions', label: '权限资源', permission: MENU_PERMISSIONS.adminPermissions, icon: <KeyOutlined /> },
  { key: '/admin/role-permissions', path: '/admin/role-permissions', label: '授权矩阵', permission: MENU_PERMISSIONS.adminRolePermissions, icon: <SafetyCertificateOutlined /> },
  { key: '/admin/exhibitors', path: '/admin/exhibitors', label: '展商管理', permission: MENU_PERMISSIONS.adminExhibitors, icon: <ShopOutlined /> },
  { key: '/admin/registrations', path: '/admin/registrations', label: '报名审核', permission: MENU_PERMISSIONS.adminRegistrations, icon: <AuditOutlined /> },
  { key: '/admin/orders', path: '/admin/orders', label: '订单管理', permission: MENU_PERMISSIONS.adminOrders, icon: <OrderedListOutlined /> },
  { key: '/admin/contracts', path: '/admin/contracts', label: '合同管理', permission: MENU_PERMISSIONS.adminContracts, icon: <SolutionOutlined /> },
  { key: '/admin/payments', path: '/admin/payments', label: '付款确认', permission: MENU_PERMISSIONS.adminPayments, icon: <DollarOutlined /> },
  { key: '/admin/selections', path: '/admin/selections', label: '选位审核', permission: MENU_PERMISSIONS.adminSelections, icon: <EnvironmentOutlined /> },
  { key: '/admin/events', path: '/admin/events', label: '展会管理', permission: MENU_PERMISSIONS.adminEvents, icon: <CalendarOutlined /> },
  { key: '/admin/booths', path: '/admin/booths', label: '展位管理', permission: MENU_PERMISSIONS.adminBooths, icon: <ApartmentOutlined /> },
  { key: '/admin/area-tiers', path: '/admin/area-tiers', label: '面积档位', permission: MENU_PERMISSIONS.adminAreaTiers, icon: <ControlOutlined /> },
  { key: '/admin/price-rules', path: '/admin/price-rules', label: '价格规则', permission: MENU_PERMISSIONS.adminPriceRules, icon: <TagsOutlined /> },
];

export function buildAdminMenu(permissionCodes: string[]) {
  const permissions = new Set(permissionCodes);
  return ADMIN_MENU_REGISTRY.filter((item) => permissions.has(item.permission));
}
