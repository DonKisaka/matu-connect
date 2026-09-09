"use client";

import { Marker, Popup } from "react-leaflet";
import MarkerClusterGroup from "react-leaflet-cluster";
import { coloredDivIcon } from "@/lib/leaflet-icons";
import type { StopDto } from "@/lib/types";

interface Props {
  stops: StopDto[];
  originStopId?: string | null;
  destinationStopId?: string | null;
  onSelect?: (stop: StopDto) => void;
}

export default function StopMarkers({ stops, originStopId, destinationStopId, onSelect }: Props) {
  const highlighted = new Set([originStopId, destinationStopId].filter(Boolean) as string[]);
  const clustered = stops.filter((s) => !highlighted.has(s.stopId));
  const special = stops.filter((s) => highlighted.has(s.stopId));

  return (
    <>
      <MarkerClusterGroup chunkedLoading>
        {clustered.map((stop) => (
          <Marker
            key={stop.stopId}
            position={[stop.latitude, stop.longitude]}
            eventHandlers={onSelect ? { click: () => onSelect(stop) } : undefined}
          >
            <Popup>{stop.stopName}</Popup>
          </Marker>
        ))}
      </MarkerClusterGroup>
      {special.map((stop) => (
        <Marker
          key={stop.stopId}
          position={[stop.latitude, stop.longitude]}
          icon={coloredDivIcon(
            stop.stopId === originStopId ? "--color-marker-origin" : "--color-marker-destination",
          )}
        >
          <Popup>{stop.stopName}</Popup>
        </Marker>
      ))}
    </>
  );
}
