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
      </CardContent>
    </Card>
  );
}
