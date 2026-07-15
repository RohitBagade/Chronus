import { useMemo, useState } from "react";
import StatusBadge from "./StatusBadge";

const STATUSES = ["ALL", "SCHEDULED", "RUNNING", "SUCCESS", "RETRYING", "FAILED", "CANCELLED"];

function fmt(dt) {
  if (!dt) return "—";
  const d = new Date(dt);
  return isNaN(d) ? dt : d.toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

export default function JobsTable({ jobs, onSelect, onCancel }) {
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("ALL");
  const [sort, setSort] = useState("scheduleTime");

  const rows = useMemo(() => {
    let r = [...jobs];
    if (status !== "ALL") r = r.filter((j) => j.status === status);
    if (q.trim()) {
      const s = q.toLowerCase();
      r = r.filter((j) => `${j.jobType} ${j.command} ${j.jobId}`.toLowerCase().includes(s));
    }
    r.sort((a, b) => {
      if (sort === "status") return (a.status || "").localeCompare(b.status || "");
      if (sort === "id") return b.jobId - a.jobId;
      return new Date(b.scheduleTime) - new Date(a.scheduleTime);
    });
    return r;
  }, [jobs, q, status, sort]);

  return (
    <div className="rounded-2xl border border-line bg-panel">
      <div className="flex flex-wrap items-center gap-3 border-b border-line p-4">
        <h2 className="mr-auto font-mono text-sm font-semibold uppercase tracking-wider text-muted">
          Jobs <span className="text-ink">{rows.length}</span>
        </h2>
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search…"
          className="h-9 w-40 rounded-lg border border-line bg-base px-3 text-sm outline-none focus:border-accent/60"
        />
        <select value={status} onChange={(e) => setStatus(e.target.value)} className={ctrl}>
          {STATUSES.map((s) => <option key={s} value={s}>{s === "ALL" ? "All statuses" : s}</option>)}
        </select>
        <select value={sort} onChange={(e) => setSort(e.target.value)} className={ctrl}>
          <option value="scheduleTime">Newest run</option>
          <option value="status">Status</option>
          <option value="id">Job ID</option>
        </select>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs uppercase tracking-wider text-muted">
              <th className="px-4 py-2.5 font-medium">Job</th>
              <th className="px-4 py-2.5 font-medium">Command</th>
              <th className="px-4 py-2.5 font-medium">Run at</th>
              <th className="px-4 py-2.5 font-medium">Recur</th>
              <th className="px-4 py-2.5 font-medium">Attempts</th>
              <th className="px-4 py-2.5 font-medium">Status</th>
              <th className="px-4 py-2.5"></th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-muted">No jobs yet — schedule one on the left.</td></tr>
            )}
            {rows.map((j) => (
              <tr
                key={j.jobId}
                onClick={() => onSelect(j.jobId)}
                className="cursor-pointer border-t border-line/60 hover:bg-panel2/60"
              >
                <td className="px-4 py-3 font-mono">
                  <span className="text-muted">#{j.jobId}</span> <span className="text-ink">{j.jobType}</span>
                </td>
                <td className="px-4 py-3 font-mono text-muted">{j.command}</td>
                <td className="px-4 py-3 text-muted">{fmt(j.scheduleTime)}</td>
                <td className="px-4 py-3 text-muted">{j.recurrence || "once"}</td>
                <td className="px-4 py-3 font-mono text-muted">{j.attemptCount}/{j.maxAttempts}</td>
                <td className="px-4 py-3"><StatusBadge status={j.status} /></td>
                <td className="px-4 py-3 text-right">
                  {j.status !== "CANCELLED" && j.status !== "SUCCESS" && (
                    <button
                      onClick={(e) => { e.stopPropagation(); onCancel(j.jobId); }}
                      className="rounded-md border border-line px-2.5 py-1 text-xs text-muted transition hover:border-err/50 hover:text-err"
                    >
                      Cancel
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const ctrl = "h-9 rounded-lg border border-line bg-base px-2.5 text-sm outline-none focus:border-accent/60 [color-scheme:dark]";
