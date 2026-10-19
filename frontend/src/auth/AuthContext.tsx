import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { apiRequest } from "../api/client";

export interface AuthUser {
  id: string;
  institutionId: string;
  email: string;
  displayName: string;
  roles: string[];
}

interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresAt: string;
  user: AuthUser;
}

interface LoginCredentials {
  institutionCode: string;
  email: string;
  password: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  accessToken: string | null;
  login(credentials: LoginCredentials): Promise<void>;
  logout(): void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthResponse | null>(null);

  const login = useCallback(async (credentials: LoginCredentials) => {
    const response = await apiRequest<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
    });
    setSession(response);
  }, []);

  const logout = useCallback(() => setSession(null), []);
  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      accessToken: session?.accessToken ?? null,
      login,
      logout,
    }),
    [login, logout, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider.");
  }
  return context;
}
