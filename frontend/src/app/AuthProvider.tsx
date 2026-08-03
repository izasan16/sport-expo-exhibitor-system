import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  clearAccessToken,
  clearStoredActiveRole,
  fetchCurrentUser,
  getAccessToken,
  getStoredActiveRole,
  loginWithPassword,
  logoutRequest,
  setAccessToken,
  setStoredActiveRole,
} from './auth';
import { resolveActiveRole, resolveRestoredActiveRole } from './roleRouting';
import type { CurrentUser, RoleCode } from './types/auth';

type AuthContextValue = {
  ready: boolean;
  user: CurrentUser | null;
  permissions: string[];
  activeRole: RoleCode | null;
  login: (username: string, password: string) => Promise<CurrentUser>;
  logout: () => Promise<void>;
  switchRole: (role: RoleCode) => void;
  hasPermission: (permission: string) => boolean;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function clearSessionState() {
  clearAccessToken();
  clearStoredActiveRole();
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [activeRole, setActiveRole] = useState<RoleCode | null>(null);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      setReady(true);
      return;
    }

    let cancelled = false;
    fetchCurrentUser(token)
      .then((currentUser) => {
        if (cancelled) {
          return;
        }
        const restoredRole = resolveRestoredActiveRole(currentUser, getStoredActiveRole());
        setUser(currentUser);
        setActiveRole(restoredRole);
        setStoredActiveRole(restoredRole);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        clearSessionState();
        setUser(null);
        setActiveRole(null);
      })
      .finally(() => {
        if (!cancelled) {
          setReady(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    ready,
    user,
    permissions: user?.permissions ?? [],
    activeRole,
    async login(username: string, password: string) {
      try {
        const token = await loginWithPassword(username, password);
        setAccessToken(token);
        const currentUser = await fetchCurrentUser(token);
        const nextRole = resolveActiveRole(currentUser);
        setUser(currentUser);
        setActiveRole(nextRole);
        setStoredActiveRole(nextRole);
        setReady(true);
        return currentUser;
      } catch (error) {
        clearSessionState();
        setUser(null);
        setActiveRole(null);
        throw error;
      }
    },
    async logout() {
      try {
        await logoutRequest();
      } finally {
        clearSessionState();
        setUser(null);
        setActiveRole(null);
        setReady(true);
      }
    },
    switchRole(role: RoleCode) {
      if (!user) {
        return;
      }
      if (!user.activeAvailableRoles.includes(role)) {
        return;
      }
      setActiveRole(role);
      setStoredActiveRole(role);
    },
    hasPermission(permission: string) {
      if (!permission) {
        return true;
      }
      return user?.permissions.includes(permission) ?? false;
    },
  }), [activeRole, ready, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
