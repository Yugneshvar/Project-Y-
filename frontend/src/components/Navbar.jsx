import logo from "../assets/logo.svg";

export default function Navbar({ email, onLogout, onMenu }) {
  return (
    <header className="navbar">
      <button className="icon-btn menu-btn" onClick={onMenu} aria-label="Open menu">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
      </button>
      <div className="brand">
        <img src={logo} alt="" width="30" height="30" />
        <span>SyncSphere</span>
      </div>
      <div className="nav-right">
        <span className="nav-email" title={email}>{email}</span>
        <button className="btn btn-ghost" onClick={onLogout}>Log out</button>
      </div>
    </header>
  );
}
