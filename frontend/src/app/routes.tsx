import { lazy, Suspense } from 'react';
import { Navigate, type RouteObject } from 'react-router-dom';
import { Spin } from 'antd';
import ProtectedRoute from './ProtectedRoute';
import { useAuth } from './AuthProvider';
import { PAGE_PERMISSIONS } from './permissionRegistry';
import { getDefaultPath } from './roleRouting';
import AdminLayout from './layouts/AdminLayout';
import ExhibitorLayout from './layouts/ExhibitorLayout';
import { RedirectWithOrderId } from './routeHelpers';

const LoginPage = lazy(() => import('../pages/LoginPage'));
const Unauthorized401Page = lazy(() => import('../pages/errors/Unauthorized401Page'));
const Forbidden403Page = lazy(() => import('../pages/errors/Forbidden403Page'));
const DashboardPage = lazy(() => import('../pages/admin/DashboardPage'));
const EventListPage = lazy(() => import('../pages/admin/events/EventListPage'));
const BoothListPage = lazy(() => import('../pages/admin/booths/BoothListPage'));
const AreaTierListPage = lazy(() => import('../pages/admin/area-tiers/AreaTierListPage'));
const PriceRuleListPage = lazy(() => import('../pages/admin/price-rules/PriceRuleListPage'));
const AdminOrderListPage = lazy(() => import('../pages/admin/orders/AdminOrderListPage'));
const AdminOrderDetailPage = lazy(() => import('../pages/admin/orders/AdminOrderDetailPage'));
const ContractListPage = lazy(() => import('../pages/admin/contracts/ContractListPage'));
const PaymentConfirmPage = lazy(() => import('../pages/admin/payments/PaymentConfirmPage'));
const SelectionReviewPage = lazy(() => import('../pages/admin/selections/SelectionReviewPage'));
const AdminExhibitorListPage = lazy(() => import('../pages/admin/exhibitors/AdminExhibitorListPage'));
const AdminRegistrationListPage = lazy(() => import('../pages/admin/registrations/AdminRegistrationListPage'));
const AdminRegistrationDetailPage = lazy(() => import('../pages/admin/registrations/AdminRegistrationDetailPage'));
const SceneEditorPage = lazy(() => import('../pages/admin/scenes/SceneEditorPage'));
const UserListPage = lazy(() => import('../pages/admin/users/UserListPage'));
const RoleListPage = lazy(() => import('../pages/admin/roles/RoleListPage'));
const PermissionResourcePage = lazy(() => import('../pages/admin/permissions/PermissionResourcePage'));
const RolePermissionMatrixPage = lazy(() => import('../pages/admin/role-permissions/RolePermissionMatrixPage'));
const ExhibitorHomePage = lazy(() => import('../pages/exhibitor/HomePage'));
const ExhibitorProfilePage = lazy(() => import('../pages/exhibitor/profile/ExhibitorProfilePage'));
const RegistrationListPage = lazy(() => import('../pages/exhibitor/registrations/RegistrationListPage'));
const RegistrationCreatePage = lazy(() => import('../pages/exhibitor/registrations/RegistrationCreatePage'));
const RegistrationDetailPage = lazy(() => import('../pages/exhibitor/registrations/RegistrationDetailPage'));
const OrderCreatePage = lazy(() => import('../pages/exhibitor/orders/OrderCreatePage'));
const MyOrdersPage = lazy(() => import('../pages/exhibitor/orders/MyOrdersPage'));
const ExhibitorOrderDetailPage = lazy(
  () => import('../pages/exhibitor/orders/ExhibitorOrderDetailPage'),
);
const PaymentSubmitPage = lazy(() => import('../pages/exhibitor/payments/PaymentSubmitPage'));
const BoothSelectionPage = lazy(() => import('../pages/exhibitor/selections/BoothSelectionPage'));
const NotificationCenterPage = lazy(() => import('../pages/exhibitor/notifications/NotificationCenterPage'));

function lazyPage(node: JSX.Element) {
  return <Suspense fallback={<Spin fullscreen />}>{node}</Suspense>;
}

function adminPage(permission: string, node: JSX.Element) {
  return <ProtectedRoute requiredPermission={permission}>{lazyPage(node)}</ProtectedRoute>;
}

function RootEntry() {
  const { ready, user, activeRole } = useAuth();

  if (!ready) {
    return <Spin fullscreen />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <Navigate to={getDefaultPath(user, activeRole ?? undefined)} replace />;
}

export const routes: RouteObject[] = [
  { path: '/', element: <RootEntry /> },
  { path: '/login', element: lazyPage(<LoginPage />) },
  { path: '/401', element: lazyPage(<Unauthorized401Page />) },
  { path: '/403', element: lazyPage(<Forbidden403Page />) },
  {
    path: '/admin',
    element: <ProtectedRoute workspace="admin"><AdminLayout /></ProtectedRoute>,
    children: [
      { index: true, element: <Navigate to="/admin/dashboard" replace /> },
      { path: 'dashboard', element: adminPage(PAGE_PERMISSIONS.adminDashboard, <DashboardPage />) },
      { path: 'events/:eventId/scene', element: adminPage(PAGE_PERMISSIONS.adminEvents, <SceneEditorPage />) },
      { path: 'users', element: adminPage(PAGE_PERMISSIONS.adminUsers, <UserListPage />) },
      { path: 'roles', element: adminPage(PAGE_PERMISSIONS.adminRoles, <RoleListPage />) },
      { path: 'permissions', element: adminPage(PAGE_PERMISSIONS.adminPermissions, <PermissionResourcePage />) },
      { path: 'role-permissions', element: adminPage(PAGE_PERMISSIONS.adminRolePermissions, <RolePermissionMatrixPage />) },
      { path: 'events', element: adminPage(PAGE_PERMISSIONS.adminEvents, <EventListPage />) },
      { path: 'booths', element: adminPage(PAGE_PERMISSIONS.adminBooths, <BoothListPage />) },
      { path: 'area-tiers', element: adminPage(PAGE_PERMISSIONS.adminAreaTiers, <AreaTierListPage />) },
      { path: 'price-rules', element: adminPage(PAGE_PERMISSIONS.adminPriceRules, <PriceRuleListPage />) },
      { path: 'orders', element: adminPage(PAGE_PERMISSIONS.adminOrders, <AdminOrderListPage />) },
      { path: 'orders/:orderId', element: adminPage(PAGE_PERMISSIONS.adminOrders, <AdminOrderDetailPage />) },
      { path: 'contracts', element: adminPage(PAGE_PERMISSIONS.adminContracts, <ContractListPage />) },
      { path: 'payments', element: adminPage(PAGE_PERMISSIONS.adminPayments, <PaymentConfirmPage />) },
      { path: 'selections', element: adminPage(PAGE_PERMISSIONS.adminSelections, <SelectionReviewPage />) },
      { path: 'exhibitors', element: adminPage(PAGE_PERMISSIONS.adminExhibitors, <AdminExhibitorListPage />) },
      { path: 'registrations', element: adminPage(PAGE_PERMISSIONS.adminRegistrations, <AdminRegistrationListPage />) },
      { path: 'registrations/:registrationId', element: adminPage(PAGE_PERMISSIONS.adminRegistrations, <AdminRegistrationDetailPage />) },
    ],
  },
  {
    path: '/exhibitor',
    element: <ProtectedRoute workspace="exhibitor"><ExhibitorLayout /></ProtectedRoute>,
    children: [
      { index: true, element: lazyPage(<ExhibitorHomePage />) },
      { path: 'profile', element: lazyPage(<ExhibitorProfilePage />) },
      { path: 'registrations', element: lazyPage(<RegistrationListPage />) },
      { path: 'registrations/new', element: lazyPage(<RegistrationCreatePage />) },
      { path: 'registrations/:registrationId', element: lazyPage(<RegistrationDetailPage />) },
      { path: 'orders/new', element: lazyPage(<OrderCreatePage />) },
      { path: 'orders', element: lazyPage(<MyOrdersPage />) },
      { path: 'orders/:orderId', element: lazyPage(<ExhibitorOrderDetailPage />) },
      { path: 'orders/:orderId/payment', element: lazyPage(<PaymentSubmitPage />) },
      { path: 'orders/:orderId/selection', element: lazyPage(<BoothSelectionPage />) },
      { path: 'notifications', element: lazyPage(<NotificationCenterPage />) },
      { path: 'payments/new', element: <RedirectWithOrderId target="payment" /> },
      { path: 'selections/new', element: <RedirectWithOrderId target="selection" /> },
      { path: 'selections', element: <RedirectWithOrderId target="detail" /> },
    ],
  },
];
