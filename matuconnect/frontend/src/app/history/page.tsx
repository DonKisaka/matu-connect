"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, CircleAlert, Clock, History, RefreshCw, Repeat } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/hooks/useAuth";
import { getMyHistory } from "@/lib/api";
import type { JourneySearchEntry } from "@/lib/types";

type Status = "loading" | "ready" | "error";

export default function HistoryPage() {
  const { user, status: authStatus } = useAuth();
  const [entries, setEntries] = useState<JourneySearchEntry[]>([]);
  const [status, setStatus] = useState<Status>("loading");

  // Split so the mount effect never calls setState synchronously
  // (react-hooks/set-state-in-effect): initial status is already "loading",
  // so only an explicit Refresh needs to reset it.
  const fetchHistory = useCallback(() => {
    getMyHistory(50)
      .then((found) => {
        setEntries(found);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, []);

  const load = useCallback(() => {
    setStatus("loading");
    fetchHistory();
  }, [fetchHistory]);

  useEffect(() => {
    if (user) fetchHistory();
  }, [user, fetchHistory]);

  if (authStatus === "loading") {
    return <PageShell><Skeleton className="h-48 w-full" /></PageShell>;
  }

  if (!user) {
    return (
      <PageShell>
        <Card>
          <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
            <History aria-hidden="true" className="size-8 text-muted-foreground" />
            <div>
              <p className="font-medium">Sign in to see your history</p>
              <p className="mt-1 text-sm text-muted-foreground">
                Journeys you look up while signed in are saved here.
              </p>
            </div>
            <Button render={<Link href="/login" />}>Sign in</Button>
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const loading = status === "loading";

  return (
    <PageShell>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Your journeys</h1>
          <p className="text-sm text-muted-foreground">
            Routes you have looked up, most recent first.
          </p>
        </div>
        <Button variant="secondary" onClick={load} disabled={loading}>
          <RefreshCw aria-hidden="true" className={`mr-2 size-4 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {status === "error" && (
        <Card className="mb-4 border-destructive/40">
          <CardContent className="flex items-center gap-3 py-4">
            <CircleAlert aria-hidden="true" className="size-5 shrink-0 text-destructive" />
            <p className="flex-1 text-sm">Could not load your history.</p>
            <Button variant="secondary" size="sm" onClick={load}>
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      {loading ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      ) : entries.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center">
            <History aria-hidden="true" className="size-7 text-muted-foreground" />
            <p className="font-medium">No journeys yet</p>
            <p className="max-w-sm text-sm text-muted-foreground">
              Plan a route on the map and it will appear here.
            </p>
            <Button render={<Link href="/" />} variant="secondary" size="sm" className="mt-1">
              Open the map
            </Button>
          </CardContent>
        </Card>
      ) : (
        <ul className="space-y-2">
          {entries.map((entry) => (
            <li key={entry.id}>
              <Card>
                <CardContent className="flex flex-wrap items-start justify-between gap-3 p-4">
                  <div className="min-w-0">
                    <p className="font-medium">
                      {entry.originStopName}
                      <span aria-hidden="true" className="mx-1.5 text-muted-foreground">
                        →
                      </span>
                      {entry.destinationStopName}
                    </p>
                    <p className="mt-0.5 font-mono text-xs text-muted-foreground">
                      {entry.originStopId} → {entry.destinationStopId}
                    </p>
                    <p className="mt-1.5 text-xs text-muted-foreground">
                      {formatWhen(entry.searchedAt)}
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center gap-3">
                    {entry.routeFound ? (
                      <>
                        <span className="flex items-center gap-1 text-sm">
                          <Clock aria-hidden="true" className="size-3.5 text-muted-foreground" />
                          <span className="font-mono tabular-nums">{entry.estimatedMinutes}</span>
                          <span className="text-muted-foreground">min</span>
                        </span>
                        <span className="flex items-center gap-1 text-sm">
                          <Repeat aria-hidden="true" className="size-3.5 text-muted-foreground" />
                          <span className="font-mono tabular-nums">{entry.transferCount}</span>
                          <span className="text-muted-foreground">
                            {entry.transferCount === 1 ? "transfer" : "transfers"}
                          </span>
                        </span>
                      </>
                    ) : (
                      // Text, not just colour — the outcome must survive being
                      // read by someone who cannot distinguish the hue.
                      <Badge variant="secondary" className="text-muted-foreground">
                        No route found
                      </Badge>
                    )}
                  </div>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </PageShell>
  );
}

/** Relative for anything recent, absolute once "3 days ago" stops being useful. */
function formatWhen(iso: string): string {
  const then = new Date(iso);
  const minutes = Math.round((Date.now() - then.getTime()) / 60000);

  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes} minute${minutes === 1 ? "" : "s"} ago`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;

  const days = Math.round(hours / 24);
  if (days < 7) return `${days} day${days === 1 ? "" : "s"} ago`;

  return then.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

function PageShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="min-h-dvh bg-background">
      <div className="mx-auto w-full max-w-3xl px-4 py-8">
        <Link
          href="/"
          className="mb-6 inline-flex items-center gap-2 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft aria-hidden="true" className="size-4" />
          Back to the map
        </Link>
        {children}
      </div>
    </main>
  );
}
