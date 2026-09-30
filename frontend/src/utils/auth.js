const KEY = "syncsphere_token";

export const getToken = () => localStorage.getItem(KEY);
export const setToken = (t) => localStorage.setItem(KEY, t);
export const clearToken = () => localStorage.removeItem(KEY);

function payload(token) {
  try {
    return JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
  } catch {
    return null;
  }
}

export function isTokenValid(token = getToken()) {
  if (!token) return false;
  const p = payload(token);
  if (!p) return false;
  return !p.exp || p.exp * 1000 > Date.now();
}

export function getEmailFromToken(token = getToken()) {
  return (token && payload(token)?.sub) || "";
}
