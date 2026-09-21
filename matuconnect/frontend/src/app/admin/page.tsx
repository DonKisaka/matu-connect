"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  CircleAlert,
  Map,
  Network,
  RefreshCw,
  Search,
  ShieldCheck,
  TrendingUp,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import StatTile from "@/components/reports/StatTile";
import SuccessRate from "@/components/reports/SuccessRate";
import { useAuth } from "@/hooks/useAuth";
import { getPopularRoutes, getUsageStats } from "@/lib/api";
import type { PopularRoute, UsageStats } from "@/lib/types";

type Status = "loading" | "ready" | "error";

export default function AdminPage() {
  const { user, status: authStatus, isAdmin } = useAuth();

  const [stats, setStats] = useState<UsageStats | null>(null);
  const [routes, setRoutes] = useState<PopularRoute[]>([]);
  const [status, setStatus] = useState<Status>("loading");

  // Split so the mount effect never calls setState synchronously
  // (react-hooks/set-state-in-effect): initial status is already "loading",
  // so only an explicit Refresh needs to reset it.
  const fetchReports = useCallback(() => {
    Promise.all([getUsageStats(), getPopularRoutes(10)])
      .then(([usage, popular]) => {
        setStats(usage);
        setRoutes(popular);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, []);

  const load = useCallback(() => {
    setStatus("loading");
    fetchReports();
  }, [fetchReports]);

  useEffect(() => {
    if (isAdmin) fetchReports();
  }, [isAdmin, fetchReports]);

  if (authStatus === "loading") {
    return <PageShell><Skeleton className="h-64 w-full" /></PageShell>;
  }

  // Authorisation is enforced by the server; this only avoids showing an
  // administrator screen to someone who would get 403 from every call on it.
  if (!isAdmin) {
    return (
      <PageShell>
        <Card>
          <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
            <ShieldCheck aria-hidden="true" className="size-8 text-muted-foreground" />
            <div>
              <p className="font-medium">Administrators only</p>
              <p className="mt-1 text-sm text-muted-foreground">
                {user
                  ? `You are signed in as ${user.username}, which is a commuter account.`
                  : "Sign in with an administrator account to view usage reports."}
              </p>
            </div>
            <Button render={<Link href={user ? "/" : "/login"} />} variant="secondary">
              {user ? "Back to the map" : "Sign in"}
            </Button>
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
          <h1 className="text-2xl font-semibold tracking-tight">Admin dashboard</h1>
          <p className="text-sm text-muted-foreground">
            Usage of the system and the shape of the network behind it.
          </p>
        </div>
        <Button variant="secondary" onClick={load} disabled={loading}>
          <RefreshCw aria-hidden="true" className={`mr-2 size-4 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {status === "error" && (
        <Card className="mb-6 border-destructive/40">
          <CardContent className="flex items-center gap-3 py-4">
            <CircleAlert aria-hidden="true" className="size-5 shrink-0 text-destructive" />
            <div className="flex-1">
              <p className="text-sm font-medium">Could not load the reports</p>
              <p className="text-sm text-muted-foreground">
                The backend may not be running, or your session may have expired.
              </p>
            </div>
            <Button variant="secondary" size="sm" onClick={load}>
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      <section aria-labelledby="usage-heading" className="mb-8">
        <h2 id="usage-heading" className="sr-only">
          Usage statistics
        </h2>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatTile
            label="Registered users"
            value={stats?.totalUsers ?? 0}
            hint={stats ? `${stats.adminUsers} administrator${stats.adminUsers === 1 ? "" : "s"}` : undefined}
            icon={Users}
            loading={loading}
          />
          <StatTile
            label="Journey searches"
            value={stats?.totalSearches ?? 0}
            hint="Including signed-out visitors"
            icon={Search}
            loading={loading}
          />
          <StatTile
            label="Routes found"
            value={stats?.successfulSearches ?? 0}
            hint={
              stats && stats.totalSearches > 0
                ? `${Math.round((stats.successfulSearches / stats.totalSearches) * 100)}% of searches`
                : undefined
            }
            icon={TrendingUp}
            loading={loading}
          />
          <StatTile
            label="No route found"
            value={stats?.failedSearches ?? 0}
            hint="Demand the network did not serve"
            icon={CircleAlert}
            loading={loading}
          />
        </div>
      </section>

      <section aria-labelledby="network-heading" className="mb-8">
        <h2 id="network-heading" className="mb-3 text-sm font-medium text-muted-foreground">
          Network structure
        </h2>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
          <StatTile label="Mapped stops" value={stats?.totalStops ?? 0} icon={Map} loading={loading} />
          <StatTile
            label="On the main network"
            value={stats?.mainNetworkSize ?? 0}
            hint={
              stats && stats.totalStops > 0
                ? `${Math.round((stats.mainNetworkSize / stats.totalStops) * 100)}% mutually reachable`
                : undefined
            }
            icon={Network}
            loading={loading}
          />
          <StatTile
            label="Isolated clusters"
            value={stats?.isolatedClusterCount ?? 0}
            hint="Cut off from the main network"
            icon={CircleAlert}
            loading={loading}
          />
        </div>
      </section>

      <section aria-labelledby="routes-heading">
        <Card>
          <CardHeader>
            <CardTitle id="routes-heading" className="text-base">
              Most searched routes
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              A pair that is searched often but rarely served is a coverage gap people are actually
              hitting.
            </p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="space-y-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : routes.length === 0 ? (
              <EmptyRoutes />
            ) : (
              // Horizontal scroll rather than a broken layout on narrow screens.
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-8 text-right">#</TableHead>
                      <TableHead>Journey</TableHead>
                      <TableHead className="text-right">Searches</TableHead>
                      <TableHead className="text-right">Found</TableHead>
                      <TableHead className="text-right">Success rate</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {routes.map((route, index) => (
                      <TableRow key={`${route.originStopId}-${route.destinationStopId}`}>
                        <TableCell className="text-right font-mono text-xs text-muted-foreground tabular-nums">
                          {index + 1}
                        </TableCell>
                        <TableCell>
                          <span className="font-medium">{route.originStopName}</span>
                          <span aria-hidden="true" className="mx-1.5 text-muted-foreground">
                            →
                          </span>
                          <span className="font-medium">{route.destinationStopName}</span>
                          {/* Several stops share a name, so the ids disambiguate
                              which stages this row actually refers to. */}
                          <span className="mt-0.5 block font-mono text-xs text-muted-foreground">
                            {route.originStopId} → {route.destinationStopId}
                          </span>
                        </TableCell>
                        <TableCell className="text-right font-mono tabular-nums">
                          {route.searchCount}
                        </TableCell>
                        <TableCell className="text-right font-mono text-muted-foreground tabular-nums">
                          {route.foundCount}
                        </TableCell>
                        <TableCell>
                          <SuccessRate percent={route.successRatePercent} />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>
      </section>
    </PageShell>
  );
}

function EmptyRoutes() {
  return (
    <div className="flex flex-col items-center gap-2 py-10 text-center">
      <Search aria-hidden="true" className="size-7 text-muted-foreground" />
      <p className="font-medium">No journeys searched yet</p>
      <p className="max-w-sm text-sm text-muted-foreground">
        This fills in as people plan routes. Try the planner on the map, then come back.
      </p>
      <Button render={<Link href="/" />} variant="secondary" size="sm" className="mt-1">
        Open the map
      </Button>
    </div>
  );
}

function PageShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="min-h-dvh bg-background">
      <div className="mx-auto w-full max-w-5xl px-4 py-8">
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
