import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <main className="center-page">
      <div className="card notfound">
        <p className="big-404">404</p>
        <h1>Page not found</h1>
        <p className="muted">The page you opened does not exist or was moved.</p>
        <Link className="btn btn-primary" to="/">Back to login</Link>
      </div>
    </main>
  );
}
