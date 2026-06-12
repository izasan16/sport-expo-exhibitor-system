import { lazy, Suspense } from 'react';
import type { RouteObject } from 'react-router-dom';
import { Spin } from 'antd';
import AdminLayout from './layouts/AdminLayout';
import ExhibitorLayout from './layouts/ExhibitorLayout';

const LoginPage = lazy(() => import('../pages/LoginPage'));
const DashboardPage = lazy(() => import('../pages/admin/DashboardPage'));
const EventListPage = lazy(() => import('../pages/admin/events/EventListPage'));
const BoothListPage = lazy(() => import('../pages/admin/booths/BoothListPage'));
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
    ],
  },
  {
    path: '/exhibitor',
    element: <ExhibitorLayout />,
    children: [{ index: true, element: lazyPage(<ExhibitorHomePage />) }],
  },
];
