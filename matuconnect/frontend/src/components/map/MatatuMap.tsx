"use client";

import { MapContainer, TileLayer, ZoomControl } from "react-leaflet";
import type { ReactNode } from "react";
import StopMarkers from "./StopMarkers";
import "@/lib/leaflet-icons";
import type { StopDto } from "@/lib/types";

const NAIROBI: [number, number] = [-1.2864, 36.8172];

export interface MatatuMapProps {
  stops: StopDto[];
  coverageSlot?: ReactNode;
  originStopId?: string | null;
  destinationStopId?: string | null;
  onSelectStop?: (stop: StopDto) => void;
}

export default function MatatuMap({
  stops,
  coverageSlot,
  originStopId,
  destinationStopId,
  onSelectStop,
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
      {coverageSlot}
    </MapContainer>
  );
}
