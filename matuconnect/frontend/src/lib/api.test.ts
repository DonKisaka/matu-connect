import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, getStops, searchStops, suggestRoute, getCoverage, sendChatMessage } from "@/lib/api";

function mockFetchOnce(body: unknown, ok = true, status = 200) {
  const spy = vi.spyOn(globalThis, "fetch").mockResolvedValueOnce({
    ok,
    status,
    json: async () => body,
  } as Response);
  return spy;
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("api client", () => {
  it("getStops calls /api/stops and returns the parsed array", async () => {
    const stops = [{ stopId: "1", stopName: "Kencom", latitude: -1.28, longitude: 36.82 }];
    const spy = mockFetchOnce(stops);
    const result = await getStops();
    expect(spy).toHaveBeenCalledWith("/api/stops", expect.objectContaining({ headers: expect.any(Object) }));
    expect(result).toEqual(stops);
  });

  it("searchStops encodes the query parameter", async () => {
    const spy = mockFetchOnce([]);
    await searchStops("Odeon & 3rd");
    expect(spy).toHaveBeenCalledWith("/api/stops/search?query=Odeon%20%26%203rd", expect.any(Object));
  });

  it("suggestRoute passes origin and destination stop ids", async () => {
    const spy = mockFetchOnce({ routeFound: false, stopNamesInOrder: [], routeNamesUsed: [], estimatedRideMinutes: 0, transferCount: 0 });
    await suggestRoute("A", "B");
    expect(spy).toHaveBeenCalledWith("/api/routes/suggest?originStopId=A&destinationStopId=B", expect.any(Object));
  });

  it("getCoverage calls /api/coverage", async () => {
    const spy = mockFetchOnce({ totalStops: 1, mainNetworkSize: 1, isolatedClusterCount: 0, exampleIsolatedStops: [], worstServedStops: [] });
    await getCoverage();
    expect(spy).toHaveBeenCalledWith("/api/coverage", expect.any(Object));
  });

  it("sendChatMessage POSTs the message as JSON", async () => {
    const spy = mockFetchOnce({ reply: "hi" });
    const result = await sendChatMessage("hello");
    expect(spy).toHaveBeenCalledWith("/api/chat", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ message: "hello" }),
    }));
    expect(result).toEqual({ reply: "hi" });
  });

  it("throws ApiError with the status on a non-2xx response", async () => {
    mockFetchOnce({}, false, 503);
    await expect(getStops()).rejects.toMatchObject({ name: "ApiError", status: 503 });
    expect(ApiError).toBeDefined();
  });
});
