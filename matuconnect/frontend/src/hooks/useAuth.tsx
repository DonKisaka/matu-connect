"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import * as api from "@/lib/api";
import type { AuthUser } from "@/lib/types";

type Status = "loading" | "ready";

interface AuthState {
  user: AuthUser | null;
  status: Status;
  isAdmin: boolean;
  signIn: (username: string, password: string) => Promise<AuthUser>;
  signUp: (username: string, password: string) => Promise<AuthUser>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

/**
 * Holds the current session for the whole app.
 * <p>
 * The session itself lives in an HttpOnly cookie the browser manages, so this
 * stores no credential — only who the server says we are. On mount it asks
 * once; a signed-out answer is `null` rather than an error, because being
 * signed out is the normal state for most visitors here.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<Status>("loading");

  const loadUser = useCallback(() => {
    api
      .getCurrentUser()
      .then((found) => {
        setUser(found);
        setStatus("ready");
      })
      .catch(() => {
        // A failure to reach the backend is treated as signed out rather than
        // blocking the map and chat, which work fine without an account.
        setUser(null);
        setStatus("ready");
      });
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const signIn = useCallback(async (username: string, password: string) => {
    const signedIn = await api.login(username, password);
    setUser(signedIn);
    return signedIn;
  }, []);

  const signUp = useCallback(async (username: string, password: string) => {
    // Registering does not sign you in server-side, so follow it with a login
    // rather than leaving the user to type the same credentials twice.
    await api.register(username, password);
    const signedIn = await api.login(username, password);
    setUser(signedIn);
    return signedIn;
  }, []);

  const signOut = useCallback(async () => {
    await api.logout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        status,
        isAdmin: user?.role === "ADMIN",
        signIn,
        signUp,
        signOut,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside an AuthProvider");
  }
  return context;
}
