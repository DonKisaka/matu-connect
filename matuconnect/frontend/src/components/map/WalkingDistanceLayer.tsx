"use client";

import { CircleMarker, Popup } from "react-leaflet";
import { cssVar } from "@/lib/leaflet-icons";
import type { WalkingDistanceGapDto } from "@/lib/types";

/**
 * Renders each sampled grid point beyond walking distance of any stop.
 * Lightweight SVG circle markers rather than the divIcon markers used for
 * stops — there can be thousands of grid points, and a DOM element per
 * point would be far heavier than Leaflet's canvas/SVG circle rendering.
 */
export default function WalkingDistanceLayer({ data }: { data: WalkingDistanceGapDto }) {
  const color = cssVar("--color-marker-walk-gap");

  return (
    <>
      {data.gaps.map((gap) => (
        <CircleMarker
          key={`${gap.latitude}-${gap.longitude}`}
          center={[gap.latitude, gap.longitude]}
          radius={4}
          pathOptions={{ color, fillColor: color, fillOpacity: 0.6, weight: 1 }}
        >
          <Popup>
            {Math.round(gap.nearestStopMetres)}m from the nearest stop
          </Popup>
        </CircleMarker>
      ))}
    </>
  );
}
