import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "../services/authService.js";
import { errorMessage } from "../utils/format.js";
import logo from "../assets/logo.svg";

export default function Register() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    setBusy(true);
    try {
      await register(fullName.trim(), email.trim(), password);
      navigate("/", { replace: true, state: { registered: true } });
    } catch (err) {
      setError(err.response?.status === 409
        ? "An account with this email already exists."
        : errorMessage(err, "Could not create the account."));
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
          <p>Upload once. Download from anywhere.</p>
        </div>
      </section>
      <section className="auth-form-side">
        <form className="auth-card" onSubmit={submit}>
          <h2>Create your account</h2>
          <p className="muted">It takes less than a minute.</p>
          {error && <p className="msg error" role="alert">{error}</p>}
          <label className="field">
            <span>Full name</span>
            <input value={fullName} onChange={(e) => setFullName(e.target.value)} required autoComplete="name" placeholder="John Doe" />
          </label>
          <label className="field">
            <span>Email</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" placeholder="you@example.com" />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} autoComplete="new-password" placeholder="At least 6 characters" />
          </label>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? "Creating account..." : "Create account"}</button>
          <p className="switch">Already registered? <Link to="/">Log in</Link></p>
        </form>
      </section>
    </main>
  );
}
