"use client";

import { Marker, Popup } from "react-leaflet";
import { coloredDivIcon } from "@/lib/leaflet-icons";
import type { CoverageDto, StopDto } from "@/lib/types";

function markers(stops: StopDto[], varName: string) {
  return stops.map((stop) => (
    <Marker
      key={`${varName}-${stop.stopId}`}
      position={[stop.latitude, stop.longitude]}
      icon={coloredDivIcon(varName)}
    >
      <Popup>{stop.stopName}</Popup>
    </Marker>
  ));
}

export default function CoverageLayer({ data }: { data: CoverageDto }) {
  return (
    <>
      {markers(data.exampleIsolatedStops, "--color-marker-isolated")}
      {markers(data.worstServedStops, "--color-marker-poor")}
    </>
  );
}
