"use client";

import { useCallback, useState } from "react";
import { getCoverage } from "@/lib/api";
import type { CoverageDto } from "@/lib/types";

type Status = "idle" | "loading" | "success" | "error";

export function useCoverage(): { data: CoverageDto | null; status: Status; load: () => void } {
  const [data, setData] = useState<CoverageDto | null>(null);
  const [status, setStatus] = useState<Status>("idle");

  const load = useCallback(() => {
    setStatus("loading");
    getCoverage()
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
