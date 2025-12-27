import { useEffect, useState } from "react";
import api from "../services/api";

export default function LogViewer({ jobId, onClose }) {
  const [content, setContent] = useState("");

  useEffect(() => {
    if (!jobId) return;

    api.get(`/jobs/${jobId}/logs/content`)
      .then(res => setContent(res.data || "No logs available"))
      .catch(() => setContent("No logs found"));
  }, [jobId]);

  if (!jobId) return null;

  return (
    <div style={{ marginTop: "20px" }}>
      <h3>Logs for Job {jobId}</h3>
      <pre style={{
        background: "#111",
        color: "#0f0",
        padding: "12px",
        maxHeight: "300px",
        overflow: "auto"
      }}>
        {content}
      </pre>
      <button onClick={onClose}>Close</button>
    </div>
  );
}
