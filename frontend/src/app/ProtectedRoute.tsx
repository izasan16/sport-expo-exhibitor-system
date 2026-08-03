import { Spin } from 'antd';
import type { ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthProvider';
import { hasAdminWorkspace, hasExhibitorWorkspace } from './roleRouting';

type ProtectedRouteProps = {
  workspace?: 'admin' | 'exhibitor';
  requiredPermission?: string;
  children?: ReactNode;
};

export default function ProtectedRoute({ workspace, requiredPermission, children }: ProtectedRouteProps) {
  const { ready, user, hasPermission } = useAuth();
  const location = useLocation();

  if (!ready) {
    return <Spin fullscreen />;
  }

  if (!user) {
    const returnTo = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?returnTo=${encodeURIComponent(returnTo)}`} replace />;
  }

  if (workspace === 'admin' && !hasAdminWorkspace(user)) {
    return <Navigate to="/403?requiredPermission=workspace.admin" replace />;
  }

  if (workspace === 'exhibitor' && !hasExhibitorWorkspace(user)) {
    return <Navigate to="/403?requiredPermission=workspace.exhibitor" replace />;
  }

  if (requiredPermission && !hasPermission(requiredPermission)) {
    return <Navigate to={`/403?requiredPermission=${encodeURIComponent(requiredPermission)}`} replace state={{ from: location }} />;
  }

  return <>{children ?? <Outlet />}</>;
}
