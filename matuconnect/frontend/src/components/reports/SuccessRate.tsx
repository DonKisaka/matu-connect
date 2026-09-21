"use client";

/**
 * Share of searches for a route pair that actually produced a journey.
 * <p>
 * Shown as a bar *and* a number, never colour alone: a reader who cannot
 * distinguish the hues still gets the value, and the bar makes a column of
 * rows scannable without reading every figure. The thresholds are deliberately
 * coarse — this is meant to separate "works", "patchy" and "never works", not
 * to imply precision the underlying counts do not support.
 */
export default function SuccessRate({ percent }: { percent: number }) {
  const clamped = Math.max(0, Math.min(100, percent));

  const tone =
    clamped >= 80
      ? { bar: "bg-emerald-600", text: "text-emerald-700", label: "reliable" }
      : clamped >= 40
        ? { bar: "bg-[var(--color-brand-accent)]", text: "text-amber-700", label: "patchy" }
        : { bar: "bg-destructive", text: "text-destructive", label: "rarely served" };

  return (
    <div className="flex items-center justify-end gap-2">
      <div
        className="h-1.5 w-16 overflow-hidden rounded-full bg-muted"
        role="img"
        aria-label={`${clamped}% of searches found a route — ${tone.label}`}
      >
        <div className={`h-full rounded-full ${tone.bar}`} style={{ width: `${clamped}%` }} />
      </div>
      <span className={`w-10 text-right font-mono text-xs tabular-nums ${tone.text}`}>
        {clamped}%
      </span>
    </div>
  );
}
