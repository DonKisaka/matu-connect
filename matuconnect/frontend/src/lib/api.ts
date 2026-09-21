import type { CoverageDto, RouteAdviceDto, StopDto, ChatResponse, ChatMessage } from "@/lib/types";

export class ApiError extends Error {
  readonly status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/**
 * The backend protects state-changing requests with the double-submit cookie
 * pattern: it publishes a readable `XSRF-TOKEN` cookie which must be echoed
 * back in `X-XSRF-TOKEN`. Safe methods are exempt, so only non-GET requests
 * need it.
 */
function csrfToken(): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? "GET").toUpperCase();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((init?.headers as Record<string, string>) ?? {}),
  };

  if (!SAFE_METHODS.has(method)) {
    const token = csrfToken();
    if (token) headers["X-XSRF-TOKEN"] = token;
  }

  const res = await fetch(path, {
    ...init,
    headers,
    // Session and CSRF cookies must ride along. Same-origin is the default,
    // but stating it keeps the intent obvious.
    credentials: "same-origin",
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
