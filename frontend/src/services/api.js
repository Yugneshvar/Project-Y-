import axios from "axios";
import { getToken, clearToken } from "../utils/auth.js";

const api = axios.create({ baseURL: "/api" });

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error) => {
    const url = error.config?.url || "";
    if (error.response?.status === 401 && !url.includes("/auth/")) {
      clearToken();
      window.location.href = "/";
    }
    return Promise.reject(error);
  }
);

export default api;
