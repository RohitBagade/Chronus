import { useEffect, useState } from "react";
import { api } from "../lib/api";
import StatusBadge from "./StatusBadge";

function fmt(dt) {
  if (!dt) return "—";
  const d = new Date(dt);
  return isNaN(d) ? dt : d.toLocaleString();
}

export default function JobDetail({ jobId, onClose, onChanged }) {
  const [job, setJob] = useState(null);
  const [execs, setExecs] = useState([]);

  async function load() {
    const [j, logs] = await Promise.all([api.getJob(jobId), api.jobLogs(jobId)]);
    setJob(j);
    setExecs(Array.isArray(logs) ? logs : []);
  }

  useEffect(() => {
    load();
    const t = setInterval(load, 3000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobId]);

  const cancel = async () => {
    await api.cancelJob(jobId);
    onChanged?.();
    load();
  };

  return (
    <div className="fixed inset-0 z-30 flex justify-end bg-black/50 backdrop-blur-sm" onClick={onClose}>
      <div
        className="h-full w-full max-w-md overflow-y-auto border-l border-line bg-panel p-6"
        onClick={(e) => e.stopPropagation()}
      >
        {!job ? (
          <p className="text-muted">Loading…</p>
        ) : (
          <>
            <div className="mb-5 flex items-start justify-between">
              <div>
                <p className="font-mono text-xs text-muted">Job #{job.jobId}</p>
                <h2 className="font-mono text-xl font-bold">{job.jobType}</h2>
              </div>
              <button onClick={onClose} className="text-muted hover:text-ink">✕</button>
            </div>

            <div className="mb-6 grid grid-cols-2 gap-3 text-sm">
              <Info label="Status"><StatusBadge status={job.status} /></Info>
              <Info label="Attempts">{job.attemptCount}/{job.maxAttempts}</Info>
              <Info label="Command"><span className="font-mono">{job.command}</span></Info>
              <Info label="Recurrence">{job.recurrence || "once"}</Info>
              <Info label="Run at" wide>{fmt(job.scheduleTime)}</Info>
            </div>

            {job.status !== "CANCELLED" && job.status !== "SUCCESS" && (
              <button
                onClick={cancel}
                className="mb-6 w-full rounded-lg border border-err/40 py-2 text-sm font-medium text-err transition hover:bg-err/10"
              >
                Cancel job
              </button>
            )}

            <h3 className="mb-3 font-mono text-xs font-semibold uppercase tracking-wider text-muted">
              Execution history <span className="text-ink">{execs.length}</span>
            </h3>
            {execs.length === 0 ? (
              <p className="text-sm text-muted">No executions yet.</p>
            ) : (
              <ol className="space-y-3">
                {execs.map((ex, i) => (
                  <li key={ex.executionId || i} className="relative border-l border-line pl-4">
                    <span className={`absolute -left-[5px] top-1.5 size-2.5 rounded-full ${ex.status === "SUCCESS" ? "bg-ok" : "bg-err"}`} />
                    <div className="flex items-center justify-between">
                      <StatusBadge status={ex.status} />
                      <span className="text-xs text-muted">{fmt(ex.startTime)}</span>
                    </div>
                    <p className="mt-1 break-words font-mono text-xs text-muted">{ex.message}</p>
                  </li>
                ))}
              </ol>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function Info({ label, children, wide }) {
  return (
    <div className={wide ? "col-span-2" : ""}>
      <p className="mb-1 text-xs text-muted">{label}</p>
      <div className="text-ink">{children}</div>
    </div>
  );
}
