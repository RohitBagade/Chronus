import { createContext, useContext, useState } from "react";
import { api } from "../lib/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("chronos_token"));
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem("chronos_user");
    return raw ? JSON.parse(raw) : null;
  });

  function persist(data) {
    localStorage.setItem("chronos_token", data.token);
    localStorage.setItem("chronos_user", JSON.stringify(data.user));
    setToken(data.token);
    setUser(data.user);
  }

  async function login(email, password) {
    persist(await api.login({ email, password }));
  }

  async function signup(name, email, password) {
    persist(await api.signup({ name, email, password }));
  }

  function logout() {
    localStorage.removeItem("chronos_token");
    localStorage.removeItem("chronos_user");
    setToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ token, user, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
