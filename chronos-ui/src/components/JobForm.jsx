import { useState } from "react";
import { api } from "../lib/api";

// datetime-local value for "now + n minutes"
function inMinutes(n) {
  const d = new Date(Date.now() + n * 60000 - new Date().getTimezoneOffset() * 60000);
  return d.toISOString().slice(0, 16);
}

const COMMANDS = [
  { value: "noop", label: "succeeds instantly" },
  { value: "log:hello", label: "logs a message" },
  { value: "fail", label: "always fails (watch retries)" },
  { value: "http:https://httpbin.org/status/200", label: "HTTP GET a URL" },
  { value: "shell:echo hello", label: "shell (disabled on public demo)" },
];

export default function JobForm({ onCreated }) {
  const [form, setForm] = useState({
    jobType: "demo",
    command: "noop",
    scheduleTime: inMinutes(1),
    recurrence: "once",
  });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const body = {
        jobType: form.jobType,
        command: form.command,
        scheduleTime: form.scheduleTime.length === 16 ? form.scheduleTime + ":00" : form.scheduleTime,
      };
      if (form.recurrence !== "once") body.recurrence = form.recurrence;
      await api.createJob(body);
      onCreated?.();
      setForm((f) => ({ ...f, scheduleTime: inMinutes(1) }));
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit} className="rounded-2xl border border-line bg-panel p-5">
      <h2 className="mb-4 font-mono text-sm font-semibold uppercase tracking-wider text-muted">Submit job</h2>

      <Label text="Type">
        <input value={form.jobType} onChange={set("jobType")} className={input} />
      </Label>
      <Label text="Command">
        <input
          list="cmd-presets"
          value={form.command}
          onChange={set("command")}
          placeholder="noop · http:<url> · shell:<cmd>"
          className={input}
        />
        <datalist id="cmd-presets">
          {COMMANDS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
        </datalist>
        <span className="mt-1 block text-[11px] text-muted">Type any command — http:&lt;url&gt; and shell:&lt;cmd&gt; are real runners.</span>
      </Label>
      <div className="grid grid-cols-2 gap-3">
        <Label text="Run at">
          <input type="datetime-local" value={form.scheduleTime} onChange={set("scheduleTime")} className={input} />
        </Label>
        <Label text="Recurrence">
          <select value={form.recurrence} onChange={set("recurrence")} className={input}>
            {["once", "minutely", "hourly", "daily", "weekly"].map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        </Label>
      </div>

      {error && <p className="mt-3 text-sm text-err">{error}</p>}

      <button
        type="submit"
        disabled={busy}
        className="mt-4 w-full rounded-lg bg-accent py-2.5 font-semibold text-base text-[#04222a] transition hover:brightness-110 disabled:opacity-50"
      >
        {busy ? "Scheduling…" : "Schedule job"}
      </button>
    </form>
  );
}

const input =
  "h-10 w-full rounded-lg border border-line bg-base px-3 text-sm outline-none transition focus:border-accent/60 focus:ring-1 focus:ring-accent/30 [color-scheme:dark]";

function Label({ text, children }) {
  return (
    <label className="mb-3 block">
      <span className="mb-1.5 block text-xs font-medium text-muted">{text}</span>
      {children}
    </label>
  );
}
