// Derived alerts — surfaces jobs that need attention (retrying / failed).
export default function Notifications({ jobs, onSelect }) {
  const alerts = jobs.filter((j) => j.status === "RETRYING" || j.status === "FAILED");

  return (
    <div className="rounded-2xl border border-line bg-panel p-5">
      <h2 className="mb-4 flex items-center gap-2 font-mono text-sm font-semibold uppercase tracking-wider text-muted">
        Alerts
        {alerts.length > 0 && (
          <span className="grid size-5 place-items-center rounded-full bg-err/15 text-xs text-err">{alerts.length}</span>
        )}
      </h2>
      {alerts.length === 0 ? (
        <p className="text-sm text-muted">All clear — no failing jobs.</p>
      ) : (
        <ul className="space-y-2">
          {alerts.map((j) => (
            <li
              key={j.jobId}
              onClick={() => onSelect(j.jobId)}
              className={`flex cursor-pointer items-center gap-3 rounded-lg border px-3 py-2 text-sm transition hover:bg-panel2/60 ${
                j.status === "FAILED" ? "border-err/30" : "border-warn/30"
              }`}
            >
              <span className={`size-2 rounded-full ${j.status === "FAILED" ? "bg-err" : "bg-warn animate-pulse-dot"}`} />
              <span className="font-mono text-xs text-muted">#{j.jobId}</span>
              <span className="flex-1 truncate">{j.jobType} · {j.command}</span>
              <span className={j.status === "FAILED" ? "text-err" : "text-warn"}>
                {j.status === "FAILED" ? "Failed" : `Retry ${j.attemptCount}/${j.maxAttempts}`}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
