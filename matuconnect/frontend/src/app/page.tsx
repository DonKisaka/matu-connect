"use client";

import { useCallback, useState } from "react";
import dynamic from "next/dynamic";
import MapView from "@/components/map/MapView";
import CoverageStats from "@/components/map/CoverageStats";
import RoutePlanner from "@/components/map/RoutePlanner";
import { Button } from "@/components/ui/button";
import type { StopDto } from "@/lib/types";
import { useStops } from "@/hooks/useStops";
import { useCoverage } from "@/hooks/useCoverage";

const CoverageLayer = dynamic(() => import("@/components/map/CoverageLayer"), { ssr: false });

export default function Home() {
  const { stops, status } = useStops();
  const coverage = useCoverage();
  const [showCoverage, setShowCoverage] = useState(false);
  const [originId, setOriginId] = useState<string | null>(null);
  const [destId, setDestId] = useState<string | null>(null);

  const handleSelectionChange = useCallback(
    (origin: StopDto | null, destination: StopDto | null) => {
      setOriginId(origin?.stopId ?? null);
      setDestId(destination?.stopId ?? null);
    },
    [],
  );

  function toggleCoverage() {
    const next = !showCoverage;
    setShowCoverage(next);
    if (next && coverage.status === "idle") {
      coverage.load();
    }
  }

  return (
    <main className="h-screen w-screen">
      {status === "error" && (
        <div className="absolute left-1/2 top-4 z-[1000] -translate-x-1/2 rounded bg-red-600 px-3 py-1 text-white">
          Could not load stops.
        </div>
      )}

      <div className="absolute left-4 top-4 z-[1000]">
        <Button variant="outline" onClick={toggleCoverage}>
          {showCoverage ? "Hide coverage" : "Show coverage"}
        </Button>
        {showCoverage && coverage.status === "error" && (
          <p className="mt-2 rounded bg-red-600 px-2 py-1 text-xs text-white">
            Could not load coverage.
          </p>
        )}
        <div className="mt-2">
          <RoutePlanner onSelectionChange={handleSelectionChange} />
        </div>
      </div>

      {showCoverage && coverage.status === "success" && coverage.data && (
        <div className="absolute right-4 top-4 z-[1000]">
          <CoverageStats data={coverage.data} />
        </div>
      )}

      <MapView
        stops={stops}
        originStopId={originId}
        destinationStopId={destId}
        coverageSlot={
          showCoverage && coverage.data ? <CoverageLayer data={coverage.data} /> : undefined
        }
      />
    </main>
  );
}
