import type {
  AuthUser,
  ChatMessage,
  ChatResponse,
  CoverageDto,
  JourneySearchEntry,
  PopularRoute,
  RouteAdviceDto,
  StopDto,
  UsageStats,
} from "@/lib/types";

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

/**
 * Asks the server for a fresh CSRF cookie.
 * <p>
 * Any safe request will do — the backend writes the token on every response —
 * so this is deliberately the cheapest endpoint available.
 */
async function refreshCsrfToken(): Promise<void> {
  try {
    await fetch("/api/auth/me", { method: "GET", credentials: "same-origin" });
  } catch {
    // Offline or backend down; the caller's own error is the useful one.
  }
}

async function send(path: string, method: string, init?: RequestInit): Promise<Response> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((init?.headers as Record<string, string>) ?? {}),
  };

  if (!SAFE_METHODS.has(method)) {
    const token = csrfToken();
    if (token) headers["X-XSRF-TOKEN"] = token;
  }

  return fetch(path, {
    ...init,
    headers,
    // Session and CSRF cookies must ride along. Same-origin is the default,
    // but stating it keeps the intent obvious.
    credentials: "same-origin",
  });
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? "GET").toUpperCase();

  let res = await send(path, method, init);

  // A browser left open across a backend restart still holds the previous
  // run's CSRF cookie, which the new server rejects — and a rejected token
  // arrives as 401/403, indistinguishable from bad credentials. Fetch a fresh
  // token and retry once, so a stale cookie cannot masquerade as a wrong
  // password. Only unsafe methods carry a token, so only they can hit this.
  if (!res.ok && !SAFE_METHODS.has(method) && (res.status === 401 || res.status === 403)) {
    await refreshCsrfToken();
    if (csrfToken()) {
      res = await send(path, method, init);
    }
  }

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
/**
 * Auth endpoints. The session lives in a cookie the browser manages, so none
 * of these return a token for the client to store.
 */

export function register(username: string, password: string): Promise<AuthUser> {
  return request<AuthUser>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export function login(username: string, password: string): Promise<AuthUser> {
  return request<AuthUser>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

/**
 * Who the caller is, or null when signed out. A 401 here is the expected
 * signed-out answer rather than a failure, so it is translated instead of
 * thrown — every caller would otherwise have to catch it.
 */
export async function getCurrentUser(): Promise<AuthUser | null> {
  try {
    return await request<AuthUser>("/api/auth/me");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

/** Logout returns 204 with no body, so it cannot go through `request`. */
export async function logout(): Promise<void> {
  const token = csrfToken();
  const res = await fetch("/api/auth/logout", {
    method: "POST",
    headers: token ? { "X-XSRF-TOKEN": token } : {},
    credentials: "same-origin",
  });
  if (!res.ok) {
    throw new ApiError(res.status, `Logout failed with ${res.status}`);
  }
}

/** Reporting. Administrator-only — a commuter receives 403. */

export function getUsageStats(): Promise<UsageStats> {
  return request<UsageStats>("/api/reports/usage");
}

export function getPopularRoutes(limit = 10): Promise<PopularRoute[]> {
  return request<PopularRoute[]>(`/api/reports/popular-routes?limit=${limit}`);
}

/** The signed-in user's own journey history. */
export function getMyHistory(limit = 20): Promise<JourneySearchEntry[]> {
  return request<JourneySearchEntry[]>(`/api/me/history?limit=${limit}`);
}

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
