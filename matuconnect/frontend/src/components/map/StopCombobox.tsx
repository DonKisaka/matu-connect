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

export default function StopCombobox({ label, testId, value, onChange }: Props) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<StopDto[]>([]);
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
          return;
        }
        searchStops(trimmed)
          .then(setResults)
          .catch(() => setResults([]));
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
        <CommandList>
          {results.length === 0 ? (
            <CommandEmpty>No matches</CommandEmpty>
          ) : (
            results.map((stop) => (
              <CommandItem
                key={stop.stopId}
                value={stop.stopId}
                onSelect={() => {
                  onChange(stop);
                  setQuery("");
                  setResults([]);
                }}
              >
                {stop.stopName}
              </CommandItem>
            ))
          )}
        </CommandList>
      </Command>
    </div>
  );
}
