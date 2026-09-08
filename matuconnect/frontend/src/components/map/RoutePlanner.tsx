"use client";

import { useEffect, useState } from "react";
import StopCombobox from "./StopCombobox";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { suggestRoute } from "@/lib/api";
import type { RouteAdviceDto, StopDto } from "@/lib/types";

interface Props {
  onSelectionChange: (origin: StopDto | null, destination: StopDto | null) => void;
}

export default function RoutePlanner({ onSelectionChange }: Props) {
  const [origin, setOrigin] = useState<StopDto | null>(null);
  const [destination, setDestination] = useState<StopDto | null>(null);
  const [result, setResult] = useState<RouteAdviceDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    onSelectionChange(origin, destination);
  }, [origin, destination, onSelectionChange]);

  async function findRoute() {
    if (!origin || !destination) return;
    setLoading(true);
    setError(false);
    setResult(null);
    try {
      setResult(await suggestRoute(origin.stopId, destination.stopId));
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="w-72">
      <CardHeader>
        <CardTitle className="text-sm">Plan a route</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <StopCombobox label="Origin" testId="origin-input" value={origin} onChange={setOrigin} />
        <StopCombobox
          label="Destination"
          testId="destination-input"
          value={destination}
          onChange={setDestination}
        />
        <Button
          className="w-full"
          disabled={!origin || !destination || loading}
          onClick={findRoute}
        >
          {loading ? "Finding…" : "Find route"}
        </Button>

        {error && (
          <p className="text-sm text-red-600">Could not fetch a route. Try again.</p>
        )}

        {result && !result.routeFound && (
          <p className="text-sm text-muted-foreground">No route found between these stops.</p>
        )}

        {result && result.routeFound && (
          <div className="space-y-1 text-sm">
            <p>
              <span className="font-medium">Board:</span> {result.routeNamesUsed.join(" → ")}
            </p>
            <p>
              {result.estimatedRideMinutes} min · {result.transferCount} transfer
              {result.transferCount === 1 ? "" : "s"}
            </p>
            <ol className="list-decimal pl-5 text-muted-foreground">
              {result.stopNamesInOrder.map((name, i) => (
                <li key={`${name}-${i}`}>{name}</li>
              ))}
            </ol>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
