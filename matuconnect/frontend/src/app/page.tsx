"use client";

import MapView from "@/components/map/MapView";
import { useStops } from "@/hooks/useStops";

export default function Home() {
  const { stops, status } = useStops();
  return (
    <main className="h-screen w-screen">
      {status === "error" && (
        <div className="absolute left-1/2 top-4 z-[1000] -translate-x-1/2 rounded bg-red-600 px-3 py-1 text-white">
          Could not load stops.
        </div>
      )}
      <MapView stops={stops} />
    </main>
  );
}
