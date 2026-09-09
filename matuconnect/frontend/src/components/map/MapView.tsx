"use client";

import dynamic from "next/dynamic";
import type { MatatuMapProps } from "./MatatuMap";

function MapSkeleton() {
  return <div className="h-full w-full animate-pulse bg-muted" aria-label="Loading map" />;
}

const MatatuMap = dynamic(() => import("./MatatuMap"), { ssr: false, loading: MapSkeleton });

export default function MapView(props: MatatuMapProps) {
  return <MatatuMap {...props} />;
}
