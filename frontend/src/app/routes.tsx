import { lazy, Suspense } from 'react';
import type { RouteObject } from 'react-router-dom';
import { Spin } from 'antd';
import AdminLayout from './layouts/AdminLayout';
import ExhibitorLayout from './layouts/ExhibitorLayout';

const LoginPage = lazy(() => import('../pages/LoginPage'));
const DashboardPage = lazy(() => import('../pages/admin/DashboardPage'));
const EventListPage = lazy(() => import('../pages/admin/events/EventListPage'));
const BoothListPage = lazy(() => import('../pages/admin/booths/BoothListPage'));
const AreaTierListPage = lazy(() => import('../pages/admin/area-tiers/AreaTierListPage'));
const PriceRuleListPage = lazy(() => import('../pages/admin/price-rules/PriceRuleListPage'));
const AdminOrderListPage = lazy(() => import('../pages/admin/orders/AdminOrderListPage'));
const ContractListPage = lazy(() => import('../pages/admin/contracts/ContractListPage'));
const PaymentConfirmPage = lazy(() => import('../pages/admin/payments/PaymentConfirmPage'));
const SelectionReviewPage = lazy(() => import('../pages/admin/selections/SelectionReviewPage'));
const ExhibitorHomePage = lazy(() => import('../pages/exhibitor/HomePage'));

function lazyPage(node: JSX.Element) {
  return <Suspense fallback={<Spin fullscreen />}>{node}</Suspense>;
}

export const routes: RouteObject[] = [
  { path: '/', element: lazyPage(<LoginPage />) },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: lazyPage(<DashboardPage />) },
      { path: 'events', element: lazyPage(<EventListPage />) },
      { path: 'booths', element: lazyPage(<BoothListPage />) },
      { path: 'area-tiers', element: lazyPage(<AreaTierListPage />) },
      { path: 'price-rules', element: lazyPage(<PriceRuleListPage />) },
      { path: 'orders', element: lazyPage(<AdminOrderListPage />) },
      { path: 'contracts', element: lazyPage(<ContractListPage />) },
      { path: 'payments', element: lazyPage(<PaymentConfirmPage />) },
      { path: 'selections', element: lazyPage(<SelectionReviewPage />) },
    ],
  },
  {
    path: '/exhibitor',
    element: <ExhibitorLayout />,
    children: [{ index: true, element: lazyPage(<ExhibitorHomePage />) }],
  },
];
