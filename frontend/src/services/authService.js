import api from "./api.js";
import { setToken, clearToken } from "../utils/auth.js";

export async function login(email, password) {
  const { data } = await api.post("/auth/login", { email, password });
  setToken(data.token);
  return data;
}

export async function register(fullName, email, password) {
  const { data } = await api.post("/auth/register", { fullName, email, password });
  return data;
}

export function logout() {
  clearToken();
}
