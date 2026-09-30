import { useNavigate } from "react-router-dom";
import { logout as doLogout } from "../services/authService.js";
import { getEmailFromToken } from "../utils/auth.js";

export default function useAuth() {
  const navigate = useNavigate();
  const email = getEmailFromToken();
  const logout = () => {
    doLogout();
    navigate("/", { replace: true });
  };
  return { email, logout };
}
