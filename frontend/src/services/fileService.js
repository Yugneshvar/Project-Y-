import api from "./api.js";
import { normalizeFile } from "../utils/format.js";

export async function listFiles() {
  const { data } = await api.get("/files");
  const arr = Array.isArray(data) ? data : data?.files ?? [];
  return arr.map(normalizeFile);
}

export async function uploadFile(file, onProgress) {
  const form = new FormData();
  form.append("file", file);
  const { data } = await api.post("/files/upload", form, {
    onUploadProgress: (e) => {
      if (e.total && onProgress) onProgress(Math.round((e.loaded * 100) / e.total));
    },
  });
  return data;
}

export async function downloadFile(filename) {
  const res = await api.get(`/files/download/${encodeURIComponent(filename)}`, { responseType: "blob" });
  const url = window.URL.createObjectURL(res.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export async function deleteFile(filename) {
  await api.delete(`/files/delete/${encodeURIComponent(filename)}`);
}
