"use client";

import { Button } from "@/components/ui/button";
import RoutePlanner from "@/components/map/RoutePlanner";
import CoverageStats from "@/components/map/CoverageStats";
import type { CoverageDto, StopDto, WalkingDistanceGapDto } from "@/lib/types";

interface Props {
  showCoverage: boolean;
  coverageData: CoverageDto | null;
  coverageError: boolean;
  onToggleCoverage: () => void;
  showWalkingDistance: boolean;
  walkingDistanceData: WalkingDistanceGapDto | null;
  walkingDistanceError: boolean;
  onToggleWalkingDistance: () => void;
  onSelectionChange: (origin: StopDto | null, destination: StopDto | null) => void;
  onRouteFound?: (stops: StopDto[] | null) => void;
}

export default function MapOverlays({
  showCoverage,
  coverageData,
  coverageError,
  onToggleCoverage,
  showWalkingDistance,
  walkingDistanceData,
  walkingDistanceError,
  onToggleWalkingDistance,
  onSelectionChange,
  onRouteFound,
}: Props) {
  return (
    <div className="pointer-events-none absolute left-3 top-3 z-[1000] flex max-h-[calc(100%-1.5rem)] flex-col gap-2 overflow-y-auto">
      {/*
        States the map's purpose before anyone has to ask what it is for:
        see the network, plan a route, or ask the assistant — the same
        three jobs the chat panel does conversationally.
      */}
      <div className="pointer-events-auto max-w-72 rounded-lg border bg-card/95 px-3 py-2 shadow-sm backdrop-blur-sm">
        <p className="text-sm font-semibold text-foreground">MatuConnect</p>
        <p className="text-xs text-muted-foreground">
          Explore Nairobi&apos;s matatu network, plan a route, or ask the assistant.
        </p>
      </div>

      <div className="pointer-events-auto flex flex-wrap gap-2">
        <Button
          variant={showCoverage ? "default" : "secondary"}
          onClick={onToggleCoverage}
        >
          {showCoverage ? "Hide coverage gaps" : "Show coverage gaps"}
        </Button>
        <Button
          variant={showWalkingDistance ? "default" : "secondary"}
          onClick={onToggleWalkingDistance}
        >
          {showWalkingDistance ? "Hide walking-distance gaps" : "Show walking-distance gaps"}
        </Button>
      </div>
      <div className="pointer-events-auto">
        <RoutePlanner onSelectionChange={onSelectionChange} onRouteFound={onRouteFound} />
      </div>
      {showCoverage && coverageError && (
        <div className="pointer-events-auto rounded bg-red-600 px-3 py-1 text-sm text-white">
          Could not load coverage data.
        </div>
      )}
      {showCoverage && coverageData && (
        <div className="pointer-events-auto">
          <CoverageStats data={coverageData} />
        </div>
      )}
      {showWalkingDistance && walkingDistanceError && (
        <div className="pointer-events-auto rounded bg-red-600 px-3 py-1 text-sm text-white">
          Could not load walking-distance data.
        </div>
      )}
      {showWalkingDistance && walkingDistanceData && (
        <div className="pointer-events-auto max-w-64 rounded-lg border bg-card/95 px-3 py-2 text-xs text-muted-foreground shadow-sm backdrop-blur-sm">
          <p className="flex items-center gap-1.5">
            <span
              aria-hidden="true"
              className="inline-block size-2.5 rounded-full"
              style={{ backgroundColor: "var(--color-marker-walk-gap)" }}
            />
            {walkingDistanceData.gaps.length} of {walkingDistanceData.gridPointsSampled} sampled
            points are beyond {Math.round(walkingDistanceData.thresholdMetres)}m of any stop
          </p>
        </div>
      )}
    </div>
  );
}
