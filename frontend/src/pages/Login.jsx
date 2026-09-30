import { useState } from "react";
import { Link, Navigate, useNavigate, useLocation } from "react-router-dom";
import { login } from "../services/authService.js";
import { isTokenValid } from "../utils/auth.js";
import { errorMessage } from "../utils/format.js";
import logo from "../assets/logo.svg";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (isTokenValid()) return <Navigate to="/dashboard" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await login(email.trim(), password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err.response?.status === 401 || err.response?.status === 403
        ? "Email or password is incorrect."
        : errorMessage(err, "Could not log in."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-art">
        <div className="orbit o1" /><div className="orbit o2" /><div className="orbit o3" />
        <div className="auth-art-text">
          <img src={logo} alt="" width="56" height="56" />
          <h1>SyncSphere</h1>
          <p>Your files, private and always within reach.</p>
        </div>
      </section>
      <section className="auth-form-side">
        <form className="auth-card" onSubmit={submit}>
          <h2>Welcome back</h2>
          <p className="muted">Log in to open your files.</p>
          {location.state?.registered && <p className="msg success">Account created. Log in to continue.</p>}
          {error && <p className="msg error" role="alert">{error}</p>}
          <label className="field">
            <span>Email</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" placeholder="you@example.com" />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" placeholder="Your password" />
          </label>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? "Logging in..." : "Log in"}</button>
          <p className="switch">New to SyncSphere? <Link to="/register">Create an account</Link></p>
        </form>
      </section>
    </main>
  );
}
