import { useState } from "react";
import JobList from "./components/JobList";
import LogViewer from "./components/LogViewer";

export default function App() {
  const [selectedJob, setSelectedJob] = useState(null);

  return (
    <div style={{ padding: "20px" }}>
      <h1>Chronos Job Dashboard</h1>

      <JobList onViewLogs={setSelectedJob} />

      <LogViewer
        jobId={selectedJob}
        onClose={() => setSelectedJob(null)}
      />
    </div>
  );
}
