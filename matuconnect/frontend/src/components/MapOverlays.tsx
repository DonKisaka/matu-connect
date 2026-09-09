"use client";

import { Button } from "@/components/ui/button";
import RoutePlanner from "@/components/map/RoutePlanner";
import CoverageStats from "@/components/map/CoverageStats";
import type { CoverageDto, StopDto } from "@/lib/types";

interface Props {
  showCoverage: boolean;
  coverageData: CoverageDto | null;
  coverageError: boolean;
  onToggleCoverage: () => void;
  onSelectionChange: (origin: StopDto | null, destination: StopDto | null) => void;
}

export default function MapOverlays({
  showCoverage,
  coverageData,
  coverageError,
  onToggleCoverage,
  onSelectionChange,
}: Props) {
  return (
    <div className="pointer-events-none absolute left-3 top-3 z-[1000] flex max-h-[calc(100%-1.5rem)] flex-col gap-2 overflow-y-auto">
      <div className="pointer-events-auto">
        <Button
          variant={showCoverage ? "default" : "secondary"}
          onClick={onToggleCoverage}
        >
          {showCoverage ? "Hide coverage gaps" : "Show coverage gaps"}
        </Button>
      </div>
      <div className="pointer-events-auto">
        <RoutePlanner onSelectionChange={onSelectionChange} />
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
    </div>
  );
}
