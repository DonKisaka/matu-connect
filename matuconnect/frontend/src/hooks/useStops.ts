"use client";

import { useCallback, useEffect, useState } from "react";
import { getStops } from "@/lib/api";
import type { StopDto } from "@/lib/types";

type Status = "loading" | "success" | "error";

export function useStops(): { stops: StopDto[]; status: Status; reload: () => void } {
  const [stops, setStops] = useState<StopDto[]>([]);
  const [status, setStatus] = useState<Status>("loading");

  const fetchStops = useCallback(() => {
    getStops()
      .then((data) => {
        setStops(data);
        setStatus("success");
      })
      .catch(() => {
        setStops([]);
        setStatus("error");
      });
  }, []);

  const reload = useCallback(() => {
    setStatus("loading");
    fetchStops();
  }, [fetchStops]);

  useEffect(() => {
    fetchStops();
  }, [fetchStops]);

  return { stops, status, reload };
}
