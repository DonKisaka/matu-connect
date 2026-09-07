import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { useStops } from "@/hooks/useStops";
import * as api from "@/lib/api";

const sample = [{ stopId: "1", stopName: "Kencom", latitude: -1.28, longitude: 36.82 }];

beforeEach(() => {
  vi.restoreAllMocks();
});

describe("useStops", () => {
  it("loads stops on mount and exposes them with success status", async () => {
    vi.spyOn(api, "getStops").mockResolvedValue(sample);
    const { result } = renderHook(() => useStops());
    expect(result.current.status).toBe("loading");
    await waitFor(() => expect(result.current.status).toBe("success"));
    expect(result.current.stops).toEqual(sample);
  });

  it("sets error status when the fetch rejects", async () => {
    vi.spyOn(api, "getStops").mockRejectedValue(new Error("boom"));
    const { result } = renderHook(() => useStops());
    await waitFor(() => expect(result.current.status).toBe("error"));
    expect(result.current.stops).toEqual([]);
  });

  it("reload re-fetches", async () => {
    const spy = vi.spyOn(api, "getStops").mockResolvedValue(sample);
    const { result } = renderHook(() => useStops());
    await waitFor(() => expect(result.current.status).toBe("success"));
    result.current.reload();
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(2));
  });
});
