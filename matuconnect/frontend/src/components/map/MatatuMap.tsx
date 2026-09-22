"use client";

import { MapContainer, Polyline, TileLayer, ZoomControl } from "react-leaflet";
import type { ReactNode } from "react";
import StopMarkers from "./StopMarkers";
import { cssVar } from "@/lib/leaflet-icons";
import type { StopDto } from "@/lib/types";

const NAIROBI: [number, number] = [-1.2864, 36.8172];

export interface MatatuMapProps {
  stops: StopDto[];
  coverageSlot?: ReactNode;
  originStopId?: string | null;
  destinationStopId?: string | null;
  onSelectStop?: (stop: StopDto) => void;
  /**
   * The ordered stops of a found route, when there is one. Drawn as a
   * straight-line-between-stops polyline — the real road geometry lives in
   * the GTFS shapes.txt table, which nothing here queries yet, so this is a
   * deliberate simplification: it shows the journey's shape and direction,
   * not the exact road path a matatu follows.
   */
  routeStops?: StopDto[] | null;
}

export default function MatatuMap({
  stops,
  coverageSlot,
  originStopId,
  destinationStopId,
  onSelectStop,
  routeStops,
}: MatatuMapProps) {
  // Leaflet's zoom control defaults to the top-left corner, where it sits
  // underneath the route planner / coverage toggle overlay stack and clips it.
  // Bottom-left is clear: the overlays own the top-left, and the mobile chat
  // button owns the bottom-right.
  return (
    <MapContainer
      center={NAIROBI}
      zoom={12}
      className="h-full w-full"
      scrollWheelZoom
      zoomControl={false}
    >
      <ZoomControl position="bottomleft" />
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <StopMarkers
        stops={stops}
        originStopId={originStopId}
        destinationStopId={destinationStopId}
        onSelect={onSelectStop}
      />
      {routeStops && routeStops.length > 1 && (
        <Polyline
          positions={routeStops.map((s) => [s.latitude, s.longitude])}
          pathOptions={{ color: cssVar("--color-brand-accent"), weight: 4, opacity: 0.85 }}
        />
      )}
      {coverageSlot}
    </MapContainer>
  );
}
