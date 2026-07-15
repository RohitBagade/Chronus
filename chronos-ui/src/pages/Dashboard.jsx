import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";
import JobForm from "../components/JobForm";
import JobsTable from "../components/JobsTable";
import JobDetail from "../components/JobDetail";
import Notifications from "../components/Notifications";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const [jobs, setJobs] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    try {
      setJobs(await api.listJobs());
      setError("");
    } catch (e) {
      // token expired / backend down -> bounce to login
      if (e?.response?.status === 401) logout();
      else setError("Can't reach the scheduler API.");
    }
  }, [logout]);

  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 3000); // live polling
    return () => clearInterval(t);
  }, [refresh]);

  const cancel = async (id) => { await api.cancelJob(id); refresh(); };

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-20 border-b border-line bg-base/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-3 px-5 py-3.5">
          <svg viewBox="0 0 32 32" className="size-6">
            <circle cx="16" cy="16" r="13" fill="none" stroke="#22d3ee" strokeWidth="2.5" />
            <path d="M16 8v8l5 3" fill="none" stroke="#22d3ee" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
          <span className="font-mono text-lg font-bold tracking-tight">Chronos</span>
          <span className="hidden rounded-full border border-line px-2 py-0.5 text-xs text-muted sm:inline">scheduler</span>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span className="text-muted">{user?.email}</span>
            <span className="rounded-full border border-accent/30 bg-accent/10 px-2 py-0.5 text-xs text-accent">{user?.role}</span>
            <button onClick={logout} className="rounded-lg border border-line px-3 py-1.5 text-muted transition hover:border-err/50 hover:text-err">
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto grid max-w-6xl gap-5 px-5 py-6 lg:grid-cols-[320px_1fr]">
        <div className="space-y-5">
          <JobForm onCreated={refresh} />
          <Notifications jobs={jobs} onSelect={setSelected} />
        </div>
        <div>
          {error && <p className="mb-3 rounded-lg border border-err/30 bg-err/10 px-3 py-2 text-sm text-err">{error}</p>}
          <JobsTable jobs={jobs} onSelect={setSelected} onCancel={cancel} />
        </div>
      </main>

      {selected && (
        <JobDetail jobId={selected} onClose={() => setSelected(null)} onChanged={refresh} />
      )}
    </div>
  );
}
