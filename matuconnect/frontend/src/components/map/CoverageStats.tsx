"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { CoverageDto } from "@/lib/types";

export default function CoverageStats({ data }: { data: CoverageDto }) {
  const rows: [string, number][] = [
    ["Total stops", data.totalStops],
    ["On main network", data.mainNetworkSize],
    ["Isolated clusters", data.isolatedClusterCount],
  ];
  return (
    <Card className="w-56">
      <CardHeader>
        <CardTitle className="text-sm">Network coverage</CardTitle>
      </CardHeader>
      <CardContent className="space-y-1 text-sm">
        {rows.map(([label, value]) => (
          <div key={label} className="flex justify-between">
            <span className="text-muted-foreground">{label}</span>
            <span className="font-medium">{value}</span>
          </div>
        ))}

        {/*
          The toggle turns these two marker colours on, but nothing on
          screen said what they meant. A legend only makes sense while it
          is true, so it lives inside this card rather than as a permanent
          fixture on the map — it appears exactly when the colours do.
        */}
        <div className="mt-2 flex flex-col gap-1 border-t pt-2 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <span
              aria-hidden="true"
              className="inline-block size-2.5 rounded-full"
              style={{ backgroundColor: "var(--color-marker-isolated)" }}
            />
            Isolated — cut off from the main network
          </span>
          <span className="flex items-center gap-1.5">
            <span
              aria-hidden="true"
              className="inline-block size-2.5 rounded-full"
              style={{ backgroundColor: "var(--color-marker-poor)" }}
            />
            Poorly served — few route options
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
