import type { CoverageDto, RouteAdviceDto, StopDto, ChatResponse, ChatMessage } from "@/lib/types";

export class ApiError extends Error {
  readonly status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
  });
  if (!res.ok) {
    throw new ApiError(res.status, `Request to ${path} failed with ${res.status}`);
  }
  return (await res.json()) as T;
}

export function getStops(): Promise<StopDto[]> {
  return request<StopDto[]>("/api/stops");
}

export function searchStops(query: string): Promise<StopDto[]> {
  return request<StopDto[]>(`/api/stops/search?query=${encodeURIComponent(query)}`);
}

export function suggestRoute(originStopId: string, destinationStopId: string): Promise<RouteAdviceDto> {
  const params = new URLSearchParams({ originStopId, destinationStopId });
  return request<RouteAdviceDto>(`/api/routes/suggest?${params.toString()}`);
}

export function getCoverage(): Promise<CoverageDto> {
  return request<CoverageDto>("/api/coverage");
}

/**
 * The backend keeps no session, so prior turns travel with each request.
 * Locally-rendered "error" bubbles are stripped: they are UI state, not
 * anything the agent said, and replaying them would confuse it.
 */
export function sendChatMessage(
  message: string,
  history: ChatMessage[] = [],
): Promise<ChatResponse> {
  const replayable = history
    .filter((m) => m.role === "user" || m.role === "assistant")
    .map((m) => ({ role: m.role, content: m.content }));

  return request<ChatResponse>("/api/chat", {
    method: "POST",
    body: JSON.stringify({ message, history: replayable }),
  });
}
