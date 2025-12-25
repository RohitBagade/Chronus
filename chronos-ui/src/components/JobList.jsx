import { useEffect, useState } from "react";
import api from "../services/api";
import LogViewer from "./LogViewer";

export default function JobList() {
  const [jobs, setJobs] = useState([]);
  const [selectedJob, setSelectedJob] = useState(null);
  const [logContent, setLogContent] = useState("");

  const fetchJobs = async () => {
    const res = await api.get("/jobs");
    setJobs(res.data);
  };

  const fetchLogs = async (id) => {
    const res = await api.get(`/jobs/${id}/logs/content`);
    setLogContent(res.data);
  };

  const executeJob = async (id) => {
    await api.post(`/test/jobs/${id}/execute`);
    alert(`Job ${id} executed manually.`);
    fetchJobs();
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Chronos Job Dashboard</h1>
      <table className="min-w-full border">
        <thead>
          <tr className="bg-gray-100 text-left">
            <th className="p-2">Job ID</th>
            <th className="p-2">Type</th>
            <th className="p-2">Command</th>
            <th className="p-2">Schedule</th>
            <th className="p-2">Status</th>
            <th className="p-2 text-center">Actions</th>
          </tr>
        </thead>
        <tbody>
          {jobs.map((job) => (
            <tr key={job.jobId} className="border-b hover:bg-gray-50">
              <td className="p-2">{job.jobId}</td>
              <td className="p-2">{job.jobType}</td>
              <td className="p-2">{job.command}</td>
              <td className="p-2">{job.scheduleTime}</td>
              <td
                className={`p-2 ${
                  job.status === "success"
                    ? "text-green-600"
                    : job.status === "failure"
                    ? "text-red-600"
                    : "text-gray-600"
                }`}
              >
                {job.status}
              </td>
              <td className="p-2 text-center space-x-2">
                <button
                  onClick={() => executeJob(job.jobId)}
                  className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded"
                >
                  Run
                </button>
                <button
                  onClick={() => fetchLogs(job.jobId)}
                  className="bg-gray-600 hover:bg-gray-700 text-white px-3 py-1 rounded"
                >
                  View Logs
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {logContent && <LogViewer content={logContent} onClose={() => setLogContent("")} />}
    </div>
  );
}
