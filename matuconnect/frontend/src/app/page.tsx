"use client";

import { useCallback, useState } from "react";
import dynamic from "next/dynamic";
import MapView from "@/components/map/MapView";
import MapOverlays from "@/components/MapOverlays";
import ChatDock from "@/components/ChatDock";
import { useStops } from "@/hooks/useStops";
import { useCoverage } from "@/hooks/useCoverage";
import type { StopDto } from "@/lib/types";

const CoverageLayer = dynamic(() => import("@/components/map/CoverageLayer"), {
  ssr: false,
});

export default function Home() {
  const { stops, status: stopsStatus, reload } = useStops();
  const coverage = useCoverage();
  const [showCoverage, setShowCoverage] = useState(false);
  const [originId, setOriginId] = useState<string | null>(null);
  const [destId, setDestId] = useState<string | null>(null);

  const onSelectionChange = useCallback(
    (origin: StopDto | null, destination: StopDto | null) => {
      setOriginId(origin?.stopId ?? null);
      setDestId(destination?.stopId ?? null);
    },
    [],
  );

  function onToggleCoverage() {
    const next = !showCoverage;
    setShowCoverage(next);
    if (next && coverage.status === "idle") {
      coverage.load();
    }
  }

  return (
    <main className="flex h-screen w-screen overflow-hidden">
      <div className="relative flex-1">
        {stopsStatus === "loading" && (
          <div className="pointer-events-none absolute left-1/2 top-3 z-[1000] -translate-x-1/2 rounded bg-background/90 px-3 py-1 text-sm text-muted-foreground shadow">
            Loading stops…
          </div>
        )}
        {stopsStatus === "error" && (
          <div className="absolute left-1/2 top-3 z-[1000] -translate-x-1/2 rounded bg-red-600 px-3 py-1 text-sm text-white">
            Could not load stops.{" "}
            <button className="underline" onClick={reload}>
              Retry
            </button>
          </div>
        )}
        <MapOverlays
          showCoverage={showCoverage}
          coverageData={showCoverage ? coverage.data : null}
          coverageError={coverage.status === "error"}
          onToggleCoverage={onToggleCoverage}
          onSelectionChange={onSelectionChange}
        />
        <MapView
          stops={stops}
          originStopId={originId}
          destinationStopId={destId}
          coverageSlot={
            showCoverage && coverage.data ? (
              <CoverageLayer data={coverage.data} />
            ) : undefined
          }
        />
      </div>
      <ChatDock />
    </main>
  );
}
