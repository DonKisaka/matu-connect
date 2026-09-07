import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { useCoverage } from "@/hooks/useCoverage";
import * as api from "@/lib/api";

const sample = {
  totalStops: 100,
  mainNetworkSize: 90,
  isolatedClusterCount: 3,
  exampleIsolatedStops: [],
  worstServedStops: [],
};

beforeEach(() => {
  vi.restoreAllMocks();
});

describe("useCoverage", () => {
  it("starts idle and does not fetch until load is called", () => {
    const spy = vi.spyOn(api, "getCoverage").mockResolvedValue(sample);
    const { result } = renderHook(() => useCoverage());
    expect(result.current.status).toBe("idle");
    expect(spy).not.toHaveBeenCalled();
  });

  it("load fetches and exposes the data", async () => {
    vi.spyOn(api, "getCoverage").mockResolvedValue(sample);
    const { result } = renderHook(() => useCoverage());
    act(() => result.current.load());
    await waitFor(() => expect(result.current.status).toBe("success"));
    expect(result.current.data).toEqual(sample);
  });

  it("sets error status on failure", async () => {
    vi.spyOn(api, "getCoverage").mockRejectedValue(new Error("boom"));
    const { result } = renderHook(() => useCoverage());
    act(() => result.current.load());
    await waitFor(() => expect(result.current.status).toBe("error"));
  });
});
