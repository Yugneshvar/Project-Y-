import { useEffect, useMemo, useState } from "react";
import Navbar from "../components/Navbar.jsx";
import Sidebar from "../components/Sidebar.jsx";
import UploadForm from "../components/UploadForm.jsx";
import FileCard from "../components/FileCard.jsx";
import FileTable from "../components/FileTable.jsx";
import LoadingSpinner from "../components/LoadingSpinner.jsx";
import useAuth from "../hooks/useAuth.js";
import useFiles from "../hooks/useFiles.js";
import {
  registerDevice,
  getDevices,
} from "../services/deviceService";
import { errorMessage, formatSize } from "../utils/format.js";

export default function Dashboard() {
  const { email, logout } = useAuth();
  const { files, loading, error, refresh, remove, download } = useFiles();

  const [query, setQuery] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [busyName, setBusyName] = useState("");
  const [notice, setNotice] = useState(null);
  const [devices, setDevices] = useState([]);

  useEffect(() => {
    const loadDevices = async () => {
      try {
        // Keep the same device ID for this browser
        let deviceId = localStorage.getItem("deviceId");

        if (!deviceId) {
          deviceId = crypto.randomUUID();
          localStorage.setItem("deviceId", deviceId);
        }

        const device = {
          deviceId,
          deviceName: navigator.platform || "Laptop",
          deviceType: "LAPTOP",
          userEmail: email,
        };

        await registerDevice(device);

        const response = await getDevices();
        setDevices(response.data);

        console.log("✅ Device Registered");
      } catch (err) {
        console.error("❌ Device registration failed", err);
      }
    };

    if (email) {
      loadDevices();
    }
  }, [email]);

  const totalSize = useMemo(
    () => files.reduce((sum, file) => sum + Number(file.size || 0), 0),
    [files]
  );

  const sorted = useMemo(
    () =>
      [...files].sort(
        (a, b) =>
          new Date(b.uploadedAt || 0) - new Date(a.uploadedAt || 0)
      ),
    [files]
  );

  const recent = sorted.slice(0, 4);

  const filtered = sorted.filter((file) =>
    file.name.toLowerCase().includes(query.trim().toLowerCase())
  );

  const handleDownload = async (name) => {
    setBusyName(name);
    setNotice(null);

    try {
      await download(name);
    } catch (e) {
      setNotice({
        type: "error",
        text: errorMessage(e, "Download failed."),
      });
    } finally {
      setBusyName("");
    }
  };

  const handleDelete = async (name) => {
    if (!window.confirm(`Delete "${name}"? This cannot be undone.`)) {
      return;
    }

    setBusyName(name);
    setNotice(null);

    try {
      await remove(name);

      setNotice({
        type: "success",
        text: `${name} deleted.`,
      });
    } catch (e) {
      setNotice({
        type: "error",
        text: errorMessage(e, "Delete failed."),
      });
    } finally {
      setBusyName("");
    }
  };

  return (
    <div className="shell" id="top">
      <Navbar
        email={email}
        onLogout={logout}
        onMenu={() => setMenuOpen((v) => !v)}
      />

      <Sidebar
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        fileCount={files.length}
      />

      <main className="content">
        <div className="page-head">
          <h1>Your Files</h1>
          <p className="muted">
            Only you can see what is stored here.
          </p>
        </div>

        <div className="top-grid">
          <UploadForm onUploaded={refresh} />

          <section className="card summary-card">
            <h2>Storage Summary</h2>

            <p className="summary-total">
              {formatSize(totalSize)}
            </p>

            <p className="muted">
              Used across {files.length}{" "}
              {files.length === 1 ? "file" : "files"}
            </p>

            <div className="summary-bar">
              <div
                style={{
                  width: `${Math.min(
                    100,
                    files.length * 10
                  )}%`,
                }}
              />
            </div>
          </section>
        </div>

        <section className="card">
          <h2>Connected Devices</h2>

          {devices.length === 0 ? (
            <p className="empty">No devices connected.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Device</th>
                  <th>Type</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {devices.map((device) => (
                  <tr key={device.deviceId}>
                    <td>{device.deviceName}</td>
                    <td>{device.deviceType}</td>
                    <td>
                      {device.online ? "🟢 Online" : "🔴 Offline"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="card">
          <h2>Recent Files</h2>

          {loading ? (
            <LoadingSpinner label="Loading files" />
          ) : recent.length === 0 ? (
            <p className="empty">
              Nothing uploaded yet. Upload your first file.
            </p>
          ) : (
            <div className="recent-grid">
              {recent.map((file) => (
                <FileCard
                  key={file.id}
                  file={file}
                  onDownload={handleDownload}
                />
              ))}
            </div>
          )}
        </section>

        <section className="card" id="files">
          <div className="table-head">
            <h2>All Files</h2>

            <input
              className="search"
              type="search"
              placeholder="Search by file name"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>

          {error && <p className="msg error">{error}</p>}

          {notice && (
            <p className={`msg ${notice.type}`}>
              {notice.text}
            </p>
          )}

          {loading ? (
            <LoadingSpinner label="Loading files" />
          ) : (
            <FileTable
              files={filtered}
              onDownload={handleDownload}
              onDelete={handleDelete}
              busyName={busyName}
              emptyText={
                query
                  ? "No files match your search."
                  : "No files yet."
              }
            />
          )}
        </section>
      </main>
    </div>
  );
}