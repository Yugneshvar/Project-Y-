export function formatSize(bytes) {
  const n = Number(bytes) || 0;
  if (n < 1024) return `${n} B`;
  const units = ["KB", "MB", "GB", "TB"];
  let v = n / 1024, i = 0;
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
  return `${v.toFixed(v >= 10 ? 0 : 1)} ${units[i]}`;
}

export function formatDate(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d)) return String(value);
  return d.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" }) +
    ", " + d.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

export function extensionOf(name = "") {
  const i = name.lastIndexOf(".");
  return i > -1 ? name.slice(i + 1).toUpperCase().slice(0, 4) : "FILE";
}

// Accepts whatever field names the backend uses and returns one shape.
export function normalizeFile(f) {
  return {
    id: f.id ?? f.fileName ?? f.filename ?? f.name,
    name: f.fileName ?? f.filename ?? f.name ?? "unnamed",
    type: f.fileType ?? f.type ?? "",
    size: f.fileSize ?? f.size ?? 0,
    uploadedAt: f.uploadTime ?? f.uploadedAt ?? f.createdAt ?? null,
  };
}

export function errorMessage(err, fallback = "Something went wrong. Try again.") {
  const d = err?.response?.data;
  if (typeof d === "string" && d) return d;
  if (d?.message) return d.message;
  if (d?.error) return d.error;
  if (!err?.response) return "Cannot reach the server. Check that the backend is running.";
  return fallback;
}
