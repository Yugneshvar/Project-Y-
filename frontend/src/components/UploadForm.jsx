import { useRef, useState } from "react";
import { uploadFile } from "../services/fileService.js";
import { errorMessage, formatSize } from "../utils/format.js";

export default function UploadForm({ onUploaded }) {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [progress, setProgress] = useState(0);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [dragging, setDragging] = useState(false);

  const pick = (f) => {
    setFile(f || null);
    setMessage(null);
    setProgress(0);
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!file) {
      setMessage({ type: "error", text: "Choose a file first." });
      return;
    }
    setBusy(true);
    setMessage(null);
    setProgress(0);
    try {
      await uploadFile(file, setProgress);
      setMessage({ type: "success", text: `${file.name} uploaded.` });
      setFile(null);
      if (inputRef.current) inputRef.current.value = "";
      onUploaded?.();
    } catch (err) {
      setMessage({ type: "error", text: errorMessage(err, "Upload failed.") });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card upload-card" onSubmit={submit} id="upload">
      <h2>Upload a file</h2>
      <label
        className={`dropzone ${dragging ? "drag" : ""}`}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          pick(e.dataTransfer.files?.[0]);
        }}
      >
        <input ref={inputRef} type="file" onChange={(e) => pick(e.target.files?.[0])} disabled={busy} />
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M12 16V4M6 10l6-6 6 6M4 20h16" /></svg>
        {file ? (
          <span><strong>{file.name}</strong> · {formatSize(file.size)}</span>
        ) : (
          <span>Drop a file here or <u>choose one</u></span>
        )}
      </label>

      {busy && (
        <div className="progress" aria-label="Upload progress">
          <div className="progress-bar" style={{ width: `${progress}%` }} />
          <span>{progress}%</span>
        </div>
      )}

      {message && <p className={`msg ${message.type}`} role="status">{message.text}</p>}

      <button className="btn btn-primary" type="submit" disabled={busy || !file}>
        {busy ? "Uploading..." : "Upload file"}
      </button>
    </form>
  );
}
