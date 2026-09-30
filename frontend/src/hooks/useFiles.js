import { useCallback, useEffect, useState } from "react";
import { listFiles, deleteFile, downloadFile } from "../services/fileService.js";
import { errorMessage } from "../utils/format.js";

export default function useFiles() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    try {
      setError("");
      setFiles(await listFiles());
    } catch (e) {
      setError(errorMessage(e, "Could not load your files."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  const remove = async (name) => {
    await deleteFile(name);
    await refresh();
  };

  return { files, loading, error, refresh, remove, download: downloadFile };
}
