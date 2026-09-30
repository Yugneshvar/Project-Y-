import { extensionOf, formatDate, formatSize } from "../utils/format.js";

export default function FileCard({ file, onDownload }) {
  return (
    <button className="file-card" onClick={() => onDownload(file.name)} title={`Download ${file.name}`}>
      <span className="ext">{extensionOf(file.name)}</span>
      <span className="file-card-name">{file.name}</span>
      <span className="muted">{formatSize(file.size)} · {formatDate(file.uploadedAt).split(",")[0]}</span>
    </button>
  );
}
