"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, Plus, RefreshCw, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import StopCombobox from "@/components/map/StopCombobox";
import {
  createAdminRoute,
  deleteAdminRoute,
  getAdminRoutes,
  rebuildNetwork,
  updateAdminRoute,
} from "@/lib/api";
import type { AdminRouteDto, RouteEditRequest, StopDto } from "@/lib/types";

interface FormStop {
  stop: StopDto;
  minutesFromPrevious: number | null;
}

type Status = "loading" | "ready" | "error";

const emptyForm = { routeShortName: "", routeLongName: "", stops: [] as FormStop[] };

export default function RouteAdminPanel() {
  const [routes, setRoutes] = useState<AdminRouteDto[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [editingRouteId, setEditingRouteId] = useState<string | "new" | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [rebuilding, setRebuilding] = useState(false);
  const [rebuildMessage, setRebuildMessage] = useState<string | null>(null);

  const load = useCallback(() => {
    getAdminRoutes()
      .then((result) => {
        setRoutes(result);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function startCreate() {
    setForm(emptyForm);
    setFormError(null);
    setEditingRouteId("new");
  }

  function startEdit(route: AdminRouteDto) {
    setForm({
      routeShortName: route.routeShortName,
      routeLongName: route.routeLongName,
      // The stop identity/order is known, but per-hop travel time isn't
      // returned by the list endpoint — re-entering it on edit is the
      // deliberate trade-off for keeping AdminRouteDto simple.
      stops: route.stopsInOrder.map((stop, i) => ({
        stop,
        minutesFromPrevious: i === 0 ? null : 5,
      })),
    });
    setFormError(null);
    setEditingRouteId(route.routeId);
  }

  function cancelForm() {
    setEditingRouteId(null);
    setFormError(null);
  }

  function addStop(stop: StopDto | null) {
    if (!stop) return;
    setForm((f) => ({
      ...f,
      stops: [...f.stops, { stop, minutesFromPrevious: f.stops.length === 0 ? null : 5 }],
    }));
  }

  function removeStop(index: number) {
    setForm((f) => ({ ...f, stops: f.stops.filter((_, i) => i !== index) }));
  }

  function setMinutes(index: number, minutes: number) {
    setForm((f) => ({
      ...f,
      stops: f.stops.map((s, i) => (i === index ? { ...s, minutesFromPrevious: minutes } : s)),
    }));
  }

  async function submit() {
    if (form.stops.length < 2) {
      setFormError("A route needs at least 2 stops.");
      return;
    }
    if (!form.routeShortName.trim()) {
      setFormError("Give the route a short name — the number a commuter would look for.");
      return;
    }
    if (form.stops.slice(1).some((s) => !s.minutesFromPrevious || s.minutesFromPrevious <= 0)) {
      setFormError("Every stop after the first needs a positive travel time in minutes.");
      return;
    }

    const body: RouteEditRequest = {
      routeShortName: form.routeShortName.trim(),
      routeLongName: form.routeLongName.trim(),
      stops: form.stops.map((s) => ({
        stopId: s.stop.stopId,
        minutesFromPrevious: s.minutesFromPrevious,
      })),
    };

    setSaving(true);
    setFormError(null);
    try {
      if (editingRouteId === "new") {
        await createAdminRoute(body);
      } else if (editingRouteId) {
        await updateAdminRoute(editingRouteId, body);
      }
      setEditingRouteId(null);
      load();
    } catch {
      setFormError("Could not save the route. Check the server is running and try again.");
    } finally {
      setSaving(false);
    }
  }

  async function remove(routeId: string) {
    if (!confirm("Delete this route? This cannot be undone.")) return;
    try {
      await deleteAdminRoute(routeId);
      load();
    } catch {
      alert("Could not delete the route.");
    }
  }

  async function rebuild() {
    setRebuilding(true);
    setRebuildMessage(null);
    try {
      await rebuildNetwork();
      setRebuildMessage("Network rebuilt — edits are now live in the route planner and chat.");
    } catch {
      setRebuildMessage("Rebuild failed. Check the server is running and try again.");
    } finally {
      setRebuilding(false);
    }
  }

  return (
    <Card>
      <CardHeader className="flex flex-row flex-wrap items-start justify-between gap-3">
        <div>
          <CardTitle className="text-base">Manage routes</CardTitle>
          <p className="text-sm text-muted-foreground">
            Add a route using stops already on the network — for new infrastructure like the
            Nairobi Expressway, or a service the static feed doesn&apos;t know about yet.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={rebuild} disabled={rebuilding}>
          <RefreshCw aria-hidden="true" className={`mr-2 size-4 ${rebuilding ? "animate-spin" : ""}`} />
          Rebuild network
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        {rebuildMessage && (
          <p className="rounded-md border bg-muted/40 px-3 py-2 text-sm">{rebuildMessage}</p>
        )}
        <p className="text-xs text-muted-foreground">
          Saving a route does not change what commuters see immediately — click{" "}
          <span className="font-medium">Rebuild network</span> to publish every saved edit to the
          live route planner and chat agent.
        </p>

        {status === "loading" && (
          <div className="space-y-2">
            {Array.from({ length: 2 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        )}

        {status === "error" && (
          <p className="text-sm text-destructive">Could not load routes. Try again.</p>
        )}

        {status === "ready" && routes.length === 0 && editingRouteId === null && (
          <p className="text-sm text-muted-foreground">
            No routes have been added through this editor yet.
          </p>
        )}

        {status === "ready" && (
          <ul className="space-y-2">
            {routes.map((route) => (
              <li
                key={route.routeId}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2"
              >
                <div>
                  <p className="text-sm font-medium">
                    {route.routeShortName}
                    <span className="ml-2 font-normal text-muted-foreground">
                      {route.routeLongName}
                    </span>
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {route.stopsInOrder.length} stops · {route.stopsInOrder[0]?.stopName} →{" "}
                    {route.stopsInOrder[route.stopsInOrder.length - 1]?.stopName}
                  </p>
                </div>
                <div className="flex gap-2">
                  <Button size="sm" variant="secondary" onClick={() => startEdit(route)}>
                    Edit
                  </Button>
                  <Button size="sm" variant="secondary" onClick={() => remove(route.routeId)}>
                    <Trash2 aria-hidden="true" className="size-4" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {editingRouteId === null ? (
          <Button variant="secondary" size="sm" onClick={startCreate}>
            <Plus aria-hidden="true" className="mr-2 size-4" />
            Add route
          </Button>
        ) : (
          <div className="space-y-3 rounded-md border p-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium">
                {editingRouteId === "new" ? "New route" : `Editing ${editingRouteId}`}
              </p>
              <Button variant="secondary" size="sm" onClick={cancelForm}>
                <X aria-hidden="true" className="size-4" />
              </Button>
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="route-short-name">Short name (route number)</Label>
                <Input
                  id="route-short-name"
                  value={form.routeShortName}
                  onChange={(e) => setForm((f) => ({ ...f, routeShortName: e.target.value }))}
                  placeholder="e.g. 111X"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="route-long-name">Long name</Label>
                <Input
                  id="route-long-name"
                  value={form.routeLongName}
                  onChange={(e) => setForm((f) => ({ ...f, routeLongName: e.target.value }))}
                  placeholder="e.g. Kitengela via Expressway"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>Stops, in order</Label>
              <ol className="space-y-2">
                {form.stops.map((entry, i) => (
                  <li key={`${entry.stop.stopId}-${i}`} className="flex items-center gap-2">
                    <span className="w-5 shrink-0 text-right text-xs text-muted-foreground">
                      {i + 1}.
                    </span>
                    <span className="flex-1 text-sm">{entry.stop.stopName}</span>
                    {i > 0 && (
                      <div className="flex items-center gap-1">
                        <Input
                          type="number"
                          min={1}
                          className="w-16"
                          value={entry.minutesFromPrevious ?? ""}
                          onChange={(e) => setMinutes(i, Number(e.target.value))}
                        />
                        <span className="text-xs text-muted-foreground">min</span>
                      </div>
                    )}
                    <Button size="sm" variant="secondary" onClick={() => removeStop(i)}>
                      <X aria-hidden="true" className="size-3.5" />
                    </Button>
                  </li>
                ))}
              </ol>
              <StopCombobox
                label="Add a stop"
                testId="admin-add-stop"
                value={null}
                onChange={addStop}
              />
            </div>

            <Button onClick={submit} disabled={saving}>
              {saving && <Loader2 aria-hidden="true" className="mr-2 size-4 animate-spin" />}
              {saving ? "Saving…" : "Save route"}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
