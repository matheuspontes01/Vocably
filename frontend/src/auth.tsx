import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { loadSession, saveSession, setUnauthorizedHandler, type AuthResponse } from "./api";

interface AuthContextValue {
  session: AuthResponse | null;
  signIn: (session: AuthResponse) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Guarda quem está logado e disponibiliza para qualquer componente via useAuth(). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState(loadSession);

  const signIn = (newSession: AuthResponse) => {
    saveSession(newSession);
    setSession(newSession);
  };

  const signOut = () => {
    saveSession(null);
    setSession(null);
  };

  useEffect(() => setUnauthorizedHandler(signOut), []);

  return <AuthContext.Provider value={{ session, signIn, signOut }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside <AuthProvider>");
  return context;
}
