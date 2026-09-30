import { extensionOf, formatDate, formatSize } from "../utils/format.js";

export default function FileTable({ files, onDownload, onDelete, busyName, emptyText }) {
  if (files.length === 0) {
    return <p className="empty">{emptyText}</p>;
  }
  return (
    <div className="table-wrap">
      <table className="file-table">
        <thead>
          <tr>
            <th>File name</th>
            <th>Size</th>
            <th>Uploaded</th>
            <th className="right">Actions</th>
          </tr>
        </thead>
        <tbody>
          {files.map((f) => (
            <tr key={f.id}>
              <td>
                <span className="name-cell">
                  <span className="ext small">{extensionOf(f.name)}</span>
                  <span className="truncate">{f.name}</span>
                </span>
              </td>
              <td>{formatSize(f.size)}</td>
              <td>{formatDate(f.uploadedAt)}</td>
              <td className="right">
                <button className="btn btn-small" onClick={() => onDownload(f.name)} disabled={busyName === f.name}>Download</button>
                <button className="btn btn-small btn-danger" onClick={() => onDelete(f.name)} disabled={busyName === f.name}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
