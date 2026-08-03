import type { ReactNode } from 'react';
import { useAuth } from './AuthProvider';

type AccessGuardProps = {
  permission: string;
  children: ReactNode;
  fallback?: ReactNode;
};

export default function AccessGuard({ permission, children, fallback }: AccessGuardProps) {
  const { hasPermission } = useAuth();

  if (hasPermission(permission)) {
    return <>{children}</>;
  }

  if (fallback !== undefined) {
    return <>{fallback}</>;
  }

  return null;
}
