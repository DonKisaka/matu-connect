"use client";

import { useEffect, useRef, useState } from "react";
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { searchStops } from "@/lib/api";
import type { StopDto } from "@/lib/types";

interface Props {
  label: string;
  testId: string;
  value: StopDto | null;
  onChange: (stop: StopDto | null) => void;
}

/**
 * "idle"      — nothing typed yet (or too short); show no list at all.
 * "searching" — a request is in flight for the current text.
 * "done"      — results below reflect the current text; empty means no match.
 *
 * Tracking this separately from `results` matters: an empty array alone
 * cannot distinguish "haven't looked yet" from "looked and found nothing",
 * which is why an untouched box used to read "No matches".
 */
type SearchStatus = "idle" | "searching" | "done";

export default function StopCombobox({ label, testId, value, onChange }: Props) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<StopDto[]>([]);
  const [status, setStatus] = useState<SearchStatus>("idle");
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (timer.current) clearTimeout(timer.current);
    const trimmed = query.trim();
    // Schedule every state update asynchronously so nothing runs a setState
    // synchronously inside the effect body (react-hooks/set-state-in-effect).
    timer.current = setTimeout(
      () => {
        if (trimmed.length < 2) {
          setResults([]);
          setStatus("idle");
          return;
        }
        setStatus("searching");
        searchStops(trimmed)
          .then((found) => {
            setResults(found);
            setStatus("done");
          })
          .catch(() => {
            setResults([]);
            setStatus("done");
          });
      },
      trimmed.length < 2 ? 0 : 300,
    );
    return () => {
      if (timer.current) clearTimeout(timer.current);
    };
  }, [query]);

  return (
    <div className="space-y-1">
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      <Command shouldFilter={false} className="rounded-md border">
        <CommandInput
          data-testid={testId}
          value={value ? value.stopName : query}
          onValueChange={(v) => {
            onChange(null);
            setQuery(v);
          }}
          placeholder={`Search ${label.toLowerCase()}…`}
        />
        {status !== "idle" && (
          <CommandList>
            {status === "searching" && <CommandEmpty>Searching…</CommandEmpty>}
            {status === "done" && results.length === 0 && (
              <CommandEmpty>No matching stop is served by any route</CommandEmpty>
            )}
            {status === "done" &&
              results.map((stop) => (
                <CommandItem
                  key={stop.stopId}
                  value={stop.stopId}
                  onSelect={() => {
                    onChange(stop);
                    setQuery("");
                    setResults([]);
                    setStatus("idle");
                  }}
                >
                  {stop.stopName}
                </CommandItem>
              ))}
          </CommandList>
        )}
      </Command>
    </div>
  );
}
