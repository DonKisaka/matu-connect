"use client";

import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

interface Props {
  label: string;
  value: number | string;
  hint?: string;
  icon?: LucideIcon;
  /** Renders a loading placeholder of the same size, so nothing shifts. */
  loading?: boolean;
}

/**
 * One headline figure on the dashboard.
 * <p>
 * The number uses tabular figures so a column of tiles keeps its digits
 * aligned and does not jiggle as values change — the reason a data-dense
 * layout reads as steady rather than restless.
 */
export default function StatTile({ label, value, hint, icon: Icon, loading }: Props) {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-start justify-between gap-2">
          <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
            {label}
          </p>
          {Icon && <Icon aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />}
        </div>

        {loading ? (
          <Skeleton className="mt-2 h-8 w-20" />
        ) : (
          <p className="mt-1 font-mono text-2xl font-semibold tabular-nums">
            {typeof value === "number" ? value.toLocaleString() : value}
          </p>
        )}

        {hint && !loading && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
      </CardContent>
    </Card>
  );
}
