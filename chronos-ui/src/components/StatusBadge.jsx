const MAP = {
  SCHEDULED: ["Scheduled", "text-run border-run/40 bg-run/10"],
  RUNNING: ["Running", "text-accent border-accent/40 bg-accent/10"],
  SUCCESS: ["Success", "text-ok border-ok/40 bg-ok/10"],
  RETRYING: ["Retrying", "text-warn border-warn/40 bg-warn/10"],
  FAILED: ["Failed", "text-err border-err/40 bg-err/10"],
  CANCELLED: ["Cancelled", "text-muted border-line bg-panel2"],
};

export default function StatusBadge({ status }) {
  const [label, cls] = MAP[status] || [status, "text-muted border-line"];
  const live = status === "RUNNING" || status === "RETRYING";
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium ${cls}`}>
      <span className={`size-1.5 rounded-full bg-current ${live ? "animate-pulse-dot" : ""}`} />
      {label}
    </span>
  );
}
