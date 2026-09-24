"use client";

import { useCallback, useState } from "react";
import { getWalkingDistanceCoverage } from "@/lib/api";
import type { WalkingDistanceGapDto } from "@/lib/types";

type Status = "idle" | "loading" | "success" | "error";

export function useWalkingDistanceCoverage(): {
  data: WalkingDistanceGapDto | null;
  status: Status;
  load: () => void;
} {
  const [data, setData] = useState<WalkingDistanceGapDto | null>(null);
  const [status, setStatus] = useState<Status>("idle");

  const load = useCallback(() => {
    setStatus("loading");
    getWalkingDistanceCoverage()
      .then((result) => {
        setData(result);
        setStatus("success");
      })
      .catch(() => {
        setStatus("error");
      });
  }, []);

  return { data, status, load };
}
