import { lazy, Suspense } from 'react';
import { Navigate, type RouteObject } from 'react-router-dom';
import { Spin } from 'antd';
import AdminLayout from './layouts/AdminLayout';
import ExhibitorLayout from './layouts/ExhibitorLayout';
import { RedirectWithOrderId } from './routeHelpers';

const LoginPage = lazy(() => import('../pages/LoginPage'));
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
const ExhibitorHomePage = lazy(() => import('../pages/exhibitor/HomePage'));
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

export const routes: RouteObject[] = [
  { path: '/', element: lazyPage(<LoginPage />) },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <Navigate to="/admin/dashboard" replace /> },
      { path: 'dashboard', element: lazyPage(<DashboardPage />) },
      { path: 'events', element: lazyPage(<EventListPage />) },
      { path: 'booths', element: lazyPage(<BoothListPage />) },
      { path: 'area-tiers', element: lazyPage(<AreaTierListPage />) },
      { path: 'price-rules', element: lazyPage(<PriceRuleListPage />) },
      { path: 'orders', element: lazyPage(<AdminOrderListPage />) },
      { path: 'orders/:orderId', element: lazyPage(<AdminOrderDetailPage />) },
      { path: 'contracts', element: lazyPage(<ContractListPage />) },
      { path: 'payments', element: lazyPage(<PaymentConfirmPage />) },
      { path: 'selections', element: lazyPage(<SelectionReviewPage />) },
    ],
  },
  {
    path: '/exhibitor',
    element: <ExhibitorLayout />,
    children: [
      { index: true, element: lazyPage(<ExhibitorHomePage />) },
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
