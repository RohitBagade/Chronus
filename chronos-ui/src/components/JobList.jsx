import { useEffect, useState } from "react";
import api from "../services/api";

export default function JobList({ onViewLogs }) {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchJobs = async () => {
    try {
      const res = await api.get("/jobs");
      setJobs(res.data);
    } catch (err) {
      console.error("Failed to fetch jobs", err);
      alert("Failed to load jobs");
    } finally {
      setLoading(false);
    }
  };

  const executeJob = async (jobId) => {
    try {
      await api.post(`/test/jobs/${jobId}/execute`);
      alert(`Job ${jobId} triggered`);
      fetchJobs();
    } catch (err) {
      alert("Job execution failed");
    }
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  if (loading) return <p>Loading jobs...</p>;

  return (
    <table border="1" cellPadding="8" width="100%">
      <thead>
        <tr>
          <th>Job ID</th>
          <th>Type</th>
          <th>Command</th>
          <th>Schedule</th>
          <th>File</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>

      <tbody>
        {jobs.length === 0 && (
          <tr>
            <td colSpan="7" align="center">No jobs found</td>
          </tr>
        )}

        {jobs.map(job => (
          <tr key={job.jobId}>
            <td>{job.jobId}</td>
            <td>{job.jobType}</td>
            <td>{job.command}</td>
            <td>{formatDate(job.scheduleTime)}</td>
            <td>{renderFile(job)}</td>
            <td>{renderStatus(job.status)}</td>
            <td>
              {job.command === "run_java_code" && (
                <button onClick={() => executeJob(job.jobId)}>Run</button>
              )}
              <button onClick={() => onViewLogs(job.jobId)}>Logs</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/* ---------- helpers ---------- */

function formatDate(dateTime) {
  if (!dateTime) return "-";
  return dateTime.replace("T", " ").substring(0, 16);
}

function renderFile(job) {
  if (!job.filePath) return "-";
  return job.filePath.split("/").pop();
}

function renderStatus(status) {
  const color =
    status === "SUCCESS" ? "green" :
    status === "FAILED" ? "red" :
    "orange";

  return <span style={{ color }}>{status}</span>;
}
