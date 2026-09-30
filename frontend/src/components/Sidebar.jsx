export default function Sidebar({ open, onClose, fileCount }) {
  return (
    <>
      <aside className={`sidebar ${open ? "open" : ""}`}>
        <nav>
          <a className="side-link active" href="#top" onClick={onClose}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 12l9-9 9 9M5 10v10h14V10" /></svg>
            Dashboard
          </a>
          <a className="side-link" href="#upload" onClick={onClose}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 16V4M6 10l6-6 6 6M4 20h16" /></svg>
            Upload
          </a>
          <a className="side-link" href="#files" onClick={onClose}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z M14 3v6h6" /></svg>
            My files
            <span className="pill">{fileCount}</span>
          </a>
        </nav>
      </aside>
      {open && <div className="scrim" onClick={onClose} />}
    </>
  );
}
