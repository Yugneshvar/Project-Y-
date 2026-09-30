import { Navigate } from "react-router-dom";
import { isTokenValid, clearToken } from "../utils/auth.js";

export default function ProtectedRoute({ children }) {
  if (!isTokenValid()) {
    clearToken();
    return <Navigate to="/" replace />;
  }
  return children;
}
