/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        base: "#080b11",
        panel: "#0e141d",
        panel2: "#141c28",
        line: "#1e2a3a",
        ink: "#e6eef6",
        muted: "#7f90a6",
        accent: "#22d3ee",
        ok: "#34d399",
        warn: "#fbbf24",
        err: "#f87171",
        run: "#60a5fa",
      },
      fontFamily: {
        mono: ["ui-monospace", "SFMono-Regular", "Menlo", "Consolas", "monospace"],
      },
      boxShadow: {
        glow: "0 0 0 1px rgba(34,211,238,0.25), 0 10px 34px -14px rgba(34,211,238,0.4)",
      },
    },
  },
  plugins: [],
};
