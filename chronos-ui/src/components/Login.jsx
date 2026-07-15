import { useState } from "react";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { login, signup } = useAuth();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      if (mode === "login") await login(form.email, form.password);
      else await signup(form.name, form.email, form.password);
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <div className="mb-3 grid size-12 place-items-center rounded-xl border border-accent/30 bg-accent/10 shadow-glow">
            <svg viewBox="0 0 32 32" className="size-7">
              <circle cx="16" cy="16" r="13" fill="none" stroke="#22d3ee" strokeWidth="2.5" />
              <path d="M16 8v8l5 3" fill="none" stroke="#22d3ee" strokeWidth="2.5" strokeLinecap="round" />
            </svg>
          </div>
          <h1 className="font-mono text-2xl font-bold tracking-tight">Chronos</h1>
          <p className="mt-1 text-sm text-muted">Distributed job scheduler</p>
        </div>

        <form onSubmit={submit} className="rounded-2xl border border-line bg-panel/70 p-6 backdrop-blur">
          <div className="mb-5 flex rounded-lg border border-line p-1 text-sm">
            {["login", "signup"].map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => { setMode(m); setError(""); }}
                className={`flex-1 rounded-md py-1.5 font-medium capitalize transition-colors ${
                  mode === m ? "bg-accent/15 text-accent" : "text-muted hover:text-ink"
                }`}
              >
                {m}
              </button>
            ))}
          </div>

          {mode === "signup" && (
            <Field label="Name" value={form.name} onChange={set("name")} required />
          )}
          <Field label="Email" type="email" value={form.email} onChange={set("email")} required />
          <Field label="Password" type="password" value={form.password} onChange={set("password")} required />

          {error && <p className="mt-3 text-sm text-err">{error}</p>}

          <button
            type="submit"
            disabled={busy}
            className="mt-5 w-full rounded-lg bg-accent py-2.5 font-semibold text-base text-[#04222a] transition hover:brightness-110 disabled:opacity-50"
          >
            {busy ? "…" : mode === "login" ? "Sign in" : "Create account"}
          </button>
        </form>

        <p className="mt-4 text-center text-xs text-muted">
          Demo admin — <span className="font-mono text-ink">admin@chronos.dev</span> / <span className="font-mono text-ink">admin12345</span>
        </p>
      </div>
    </div>
  );
}

function Field({ label, ...props }) {
  return (
    <label className="mb-3 block">
      <span className="mb-1.5 block text-xs font-medium text-muted">{label}</span>
      <input
        {...props}
        className="h-10 w-full rounded-lg border border-line bg-base px-3 text-sm outline-none transition focus:border-accent/60 focus:ring-1 focus:ring-accent/30"
      />
    </label>
  );
}
