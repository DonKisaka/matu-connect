# MatuConnect Next.js Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Next.js web frontend for MatuConnect — a Leaflet map of the Nairobi matatu network plus a chat interface — calling the existing Spring Boot REST API, and add the one missing backend endpoint (`POST /api/chat`).

**Architecture:** Next.js 15 App Router app in `frontend/`, single split-view page (map + chat). Browser calls only relative `/api/*` URLs; `next.config.ts` rewrites proxy those to `http://localhost:8080` so there is no CORS problem and no backend CORS config. Typed `fetch` wrappers in `lib/api.ts`; React hooks for data. Visual layer built from `ui-ux-pro-max` tokens + shadcn/ui primitives + 21st.dev-generated composites. Backend gets one small `ChatController` that delegates to the already-wired `ChatClient` bean.

**Tech Stack:** Next.js 15, React 19, TypeScript, Tailwind CSS v4, shadcn/ui, react-leaflet 5 + leaflet 1.9, Vitest + React Testing Library. Backend: Spring Boot 4.1.0, Spring AI 2.0.0, Java 25, Maven.

**Spec:** `docs/superpowers/specs/2026-09-07-nextjs-frontend-design.md`

## Global Constraints

- Frontend lives in `frontend/` at the project root (`C:\Users\Administrator\Downloads\matuconnect\matuconnect\frontend`).
- Next.js 16 (installed via `create-next-app@latest`; `@latest` resolved to 16.3.4, React 19.2.8), App Router, TypeScript, `src/` dir, import alias `@/*`, npm. Node v22. (The plan was drafted saying "15"; `@latest` moved to 16. Ruled acceptable — every substantive requirement here uses App Router basics, `next/dynamic` client-component imports, `next/font`, and Tailwind v4, all identical on 16.)
- Tailwind CSS **v4** — no `tailwind.config.js`; theme tokens live in `@theme` inside `src/app/globals.css`.
- **Every** browser-side API call uses a relative URL beginning `/api/` — never `http://localhost:8080` in frontend code.
- Backend is Spring Boot 4.1.0 — use `@org.springframework.test.context.bean.override.mockito.MockitoBean`, **not** the removed `@MockBean`.
- New backend `ChatController` reuses the existing package-private `ChatRequest` / `ChatResponse` records in `com.matuconnect.controller.ChatMessages` — same package, do not make them public, do not create new DTOs.
- shadcn components are added with the `npx shadcn@latest` CLI; the shadcn MCP server is unavailable this session.
- TDD throughout: failing test first, minimal code, passing test, commit. Commit after every task.
- Work on branch `feat/nextjs-frontend` (already checked out).
- Backend commits: prefix `feat:`; run `./mvnw test` before committing backend changes. Frontend commits: prefix `feat:` / `chore:`; run `npm run lint` and `npm test` before committing.

---

### Task 1: Backend `ChatController` for `POST /api/chat`

**Files:**
- Create: `src/main/java/com/matuconnect/controller/ChatController.java`
- Test: `src/test/java/com/matuconnect/controller/ChatControllerTest.java`

**Interfaces:**
- Consumes: existing `ChatClient` bean (`org.springframework.ai.chat.client.ChatClient`), configured in `com.matuconnect.agent.MatuConnectChatConfig`. Existing records `ChatRequest(String message)` and `ChatResponse(String reply)` in `ChatMessages.java` (same package, package-private).
- Produces: HTTP endpoint `POST /api/chat`, request body `{"message": string}`, response body `{"reply": string}`, status 200. Consumed by the frontend `sendChatMessage` wrapper in Task 3.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/matuconnect/controller/ChatControllerTest.java`:

```java
package com.matuconnect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatClient chatClient;

    @Test
    void returnsAgentReplyForAMessage() throws Exception {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Board route 46 at Kencom heading west.");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How do I get to Westlands from Kencom?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Board route 46 at Kencom heading west."));
    }

    @Test
    void returnsBadRequestWhenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
```

> If the nested spec type names differ in Spring AI 2.0.0, open the imported `ChatClient` interface and use the actual return type of `prompt()` and of `.call()`. The chain called by the controller is exactly `chatClient.prompt().user(message).call().content()`.

- [ ] **Step 2: Run the test, verify it fails**

Run: `./mvnw test -Dtest=ChatControllerTest`
Expected: FAIL — compilation error, `ChatController` does not exist.

- [ ] **Step 3: Write the minimal controller**

Create `src/main/java/com/matuconnect/controller/ChatController.java`:

```java
package com.matuconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bridges the frontend chat UI to the configured MatuConnect chat agent.
 * The {@link ChatClient} bean already carries the system prompt, the
 * route-advisory tools, and the knowledge-base RAG advisor (see
 * {@code com.matuconnect.agent.MatuConnectChatConfig}); this controller
 * only forwards the user's message and returns the reply.
 *
 * <p>Blocking call — no streaming. Each request is independent; the
 * agent holds no per-user conversation memory.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
class ChatController {

    private final ChatClient chatClient;

    @PostMapping
    ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = chatClient.prompt()
                .user(request.message())
                .call()
                .content();
        return new ChatResponse(reply);
    }
}
```

- [ ] **Step 4: Run the test, verify it passes**

Run: `./mvnw test -Dtest=ChatControllerTest`
Expected: PASS — both tests green.

- [ ] **Step 5: Run the full backend test suite**

Run: `./mvnw test`
Expected: PASS — no regressions in existing tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/matuconnect/controller/ChatController.java src/test/java/com/matuconnect/controller/ChatControllerTest.java
git commit -m "feat: add ChatController exposing POST /api/chat"
```

---

### Task 2: Scaffold the Next.js app and the API proxy

**Files:**
- Create: `frontend/` (entire `create-next-app` output)
- Create: `frontend/next.config.ts` (replace generated)
- Create: `frontend/.env.local.example`
- Modify: `frontend/.gitignore` (ensure `.env.local` ignored — usually already is)
- Modify: repo root `.gitignore` — add `frontend/node_modules/` and `frontend/.next/` if the generated `frontend/.gitignore` is not picked up

**Interfaces:**
- Produces: a running Next.js dev server on `http://localhost:3000`; requests to `/api/*` proxied to `${BACKEND_URL || 'http://localhost:8080'}/api/*`. All later tasks assume this proxy exists.

- [ ] **Step 1: Scaffold**

From the project root (`C:\Users\Administrator\Downloads\matuconnect\matuconnect`):

```bash
npx create-next-app@latest frontend --typescript --tailwind --eslint --app --src-dir --import-alias "@/*" --use-npm --no-git
```

Accept defaults for any remaining prompts (Turbopack: yes is fine).

- [ ] **Step 2: Replace `frontend/next.config.ts`**

```ts
import type { NextConfig } from "next";

const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backend}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
```

- [ ] **Step 3: Create `frontend/.env.local.example`**

```
# URL of the running MatuConnect Spring Boot backend.
# Copy this file to .env.local to override the default.
BACKEND_URL=http://localhost:8080
```

- [ ] **Step 4: Verify it builds and the proxy is wired**

```bash
cd frontend
npm run build
```
Expected: build succeeds.

Then, with the backend running (`./mvnw spring-boot:run` in another terminal):

```bash
npm run dev
```
In a third terminal:
```bash
curl -s http://localhost:3000/api/stops | head -c 200
```
Expected: a JSON array of stop objects (proxied from :8080). If the backend is not running, expect a 502/connection error from the proxy — that still proves the rewrite is in place (a 404 from Next would mean it is not).

- [ ] **Step 5: Commit**

```bash
cd ..
git add frontend .gitignore
git commit -m "chore: scaffold Next.js frontend with API proxy to :8080"
```

---

### Task 3: Types, typed API client, and Vitest setup

**Files:**
- Create: `frontend/src/lib/types.ts`
- Create: `frontend/src/lib/api.ts`
- Create: `frontend/src/lib/api.test.ts`
- Create: `frontend/vitest.config.ts`
- Create: `frontend/vitest.setup.ts`
- Modify: `frontend/package.json` (add `test` script)

**Interfaces:**
- Produces:
  - `types.ts`: `StopDto { stopId: string; stopName: string; latitude: number; longitude: number }`, `RouteAdviceDto { routeFound: boolean; stopNamesInOrder: string[]; routeNamesUsed: string[]; estimatedRideMinutes: number; transferCount: number }`, `CoverageDto { totalStops: number; mainNetworkSize: number; isolatedClusterCount: number; exampleIsolatedStops: StopDto[]; worstServedStops: StopDto[] }`, `ChatResponse { reply: string }`, `ChatMessage { role: "user" | "assistant" | "error"; content: string }`.
  - `api.ts`: `class ApiError extends Error { status: number }`; `getStops(): Promise<StopDto[]>`; `searchStops(query: string): Promise<StopDto[]>`; `suggestRoute(originStopId: string, destinationStopId: string): Promise<RouteAdviceDto>`; `getCoverage(): Promise<CoverageDto>`; `sendChatMessage(message: string): Promise<ChatResponse>`.
- All later tasks import from `@/lib/api` and `@/lib/types`.

- [ ] **Step 1: Install test dependencies**

```bash
cd frontend
npm install -D vitest @vitejs/plugin-react jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

- [ ] **Step 2: Create `frontend/vitest.config.ts`**

```ts
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    globals: true,
  },
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
  },
});
```

- [ ] **Step 3: Create `frontend/vitest.setup.ts`**

```ts
import "@testing-library/jest-dom/vitest";
```

- [ ] **Step 4: Add the `test` script to `frontend/package.json`**

In `"scripts"`, add:
```json
"test": "vitest run",
"test:watch": "vitest"
```

- [ ] **Step 5: Write `frontend/src/lib/types.ts`**

```ts
export interface StopDto {
  stopId: string;
  stopName: string;
  latitude: number;
  longitude: number;
}

export interface RouteAdviceDto {
  routeFound: boolean;
  stopNamesInOrder: string[];
  routeNamesUsed: string[];
  estimatedRideMinutes: number;
  transferCount: number;
}

export interface CoverageDto {
  totalStops: number;
  mainNetworkSize: number;
  isolatedClusterCount: number;
  exampleIsolatedStops: StopDto[];
  worstServedStops: StopDto[];
}

export interface ChatResponse {
  reply: string;
}

export interface ChatMessage {
  role: "user" | "assistant" | "error";
  content: string;
}
```

- [ ] **Step 6: Write the failing test `frontend/src/lib/api.test.ts`**

```ts
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
```

- [ ] **Step 7: Run the test, verify it fails**

Run: `npm test -- src/lib/api.test.ts`
Expected: FAIL — `@/lib/api` has no exports / module not found.

- [ ] **Step 8: Write `frontend/src/lib/api.ts`**

```ts
import type { CoverageDto, RouteAdviceDto, StopDto, ChatResponse } from "@/lib/types";

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

export function sendChatMessage(message: string): Promise<ChatResponse> {
  return request<ChatResponse>("/api/chat", {
    method: "POST",
    body: JSON.stringify({ message }),
  });
}
```

> Note: the `suggestRoute` test expects `originStopId=A&destinationStopId=B` in that order. `URLSearchParams` preserves insertion order, so passing `originStopId` first matches. If a future `encodeURIComponent` edge case matters, keep the `URLSearchParams` form.

- [ ] **Step 9: Run the test, verify it passes**

Run: `npm test -- src/lib/api.test.ts`
Expected: PASS — all cases green.

- [ ] **Step 10: Lint and commit**

```bash
npm run lint
git add frontend/src/lib frontend/vitest.config.ts frontend/vitest.setup.ts frontend/package.json frontend/package-lock.json
git commit -m "feat: add typed API client, shared types, and Vitest setup"
```

---

### Task 4: Design system — `ui-ux-pro-max` tokens + shadcn primitives

**Files:**
- Modify: `frontend/src/app/globals.css` (theme tokens under `@theme`)
- Modify: `frontend/src/app/layout.tsx` (fonts, metadata, body classes)
- Create: `frontend/components.json` (shadcn config, via CLI)
- Create: `frontend/src/components/ui/*` (button, input, card, sheet, scroll-area, command — via CLI)
- Create: `frontend/src/lib/utils.ts` (shadcn `cn` helper, via CLI)
- Modify: `docs/superpowers/specs/2026-09-07-nextjs-frontend-design.md` (fill in the "Chosen style / palette / fonts" line)

**Interfaces:**
- Produces: Tailwind theme tokens (colors, radius, font families) usable as Tailwind classes; `cn` helper at `@/lib/utils`; shadcn components importable from `@/components/ui/*` — specifically `Button`, `Input`, `Card` (+ `CardHeader`, `CardContent`, `CardTitle`, `CardFooter`), `Sheet` (+ `SheetTrigger`, `SheetContent`, `SheetHeader`, `SheetTitle`), `ScrollArea`, and `Command` (+ `CommandInput`, `CommandList`, `CommandEmpty`, `CommandItem`).

- [ ] **Step 1: Run the `ui-ux-pro-max` skill**

Invoke `superpowers`-style: call the `ui-ux-pro-max` skill. Brief: "map-forward Nairobi public-transit web tool; dense data overlays on a full-screen map; a persistent chat panel; mobile-first; needs clear marker colour coding for normal / isolated / poorly-served stops and origin / destination." Ask it for: style direction, product palette + reasoning profile, one font pairing, and the 3-5 most relevant UX guidelines. Capture its output.

- [ ] **Step 2: Write the chosen tokens into `globals.css`**

Under the existing Tailwind v4 setup, add an `@theme` block with the palette and fonts from Step 1. Example shape (replace values with the skill's output):

```css
@import "tailwindcss";

@theme {
  --color-background: #ffffff;
  --color-foreground: #0f172a;
  --color-primary: #0f766e;          /* replace with ui-ux-pro-max primary */
  --color-primary-foreground: #ffffff;
  --color-muted: #f1f5f9;
  --color-border: #e2e8f0;

  /* marker semantics — used by lib/leaflet-icons.ts */
  --color-marker-stop: #2563eb;
  --color-marker-isolated: #dc2626;
  --color-marker-poor: #d97706;
  --color-marker-origin: #16a34a;
  --color-marker-destination: #7c3aed;

  --radius: 0.5rem;
  --font-sans: var(--font-sans-loaded), ui-sans-serif, system-ui, sans-serif;
}

/* dark mode: mirror the palette if ui-ux-pro-max specifies one */
```

Keep a short comment block at the top of the file recording which `ui-ux-pro-max` style was chosen.

- [ ] **Step 3: Wire fonts in `layout.tsx`**

Use `next/font` for the pairing from Step 1 (e.g. `next/font/google`), expose them as CSS variables on `<body>` (`--font-sans-loaded`, and a display font var if the pairing has one), and set `metadata.title` to `"MatuConnect"` and a one-line description.

- [ ] **Step 4: Initialise shadcn and add primitives**

```bash
cd frontend
npx shadcn@latest init
```
Answer prompts: style — as close to the `ui-ux-pro-max` direction as offered; base colour — neutral/slate; CSS variables — yes.

```bash
npx shadcn@latest add button input card sheet scroll-area command
```

- [ ] **Step 5: Verify build and a smoke render**

Temporarily edit `src/app/page.tsx` to render `<Button>Test</Button>` inside a `<Card>`, then:
```bash
npm run build
```
Expected: build succeeds, no type errors. Revert the temporary edit.

- [ ] **Step 6: Update the spec**

In `docs/superpowers/specs/2026-09-07-nextjs-frontend-design.md`, replace the "Chosen style / palette / fonts: _to be filled in_" line with the actual chosen style name, palette hex values, and font pairing.

- [ ] **Step 7: Commit**

```bash
cd ..
git add frontend docs/superpowers/specs/2026-09-07-nextjs-frontend-design.md
git commit -m "feat: set up design tokens (ui-ux-pro-max) and shadcn primitives"
```

---

### Task 5: `useStops` hook

**Files:**
- Create: `frontend/src/hooks/useStops.ts`
- Create: `frontend/src/hooks/useStops.test.tsx`

**Interfaces:**
- Consumes: `getStops` from `@/lib/api`, `StopDto` from `@/lib/types`.
- Produces: `useStops(): { stops: StopDto[]; status: "loading" | "success" | "error"; reload: () => void }`. Fetches once on mount. Used by `MatatuMap` (Task 7).

- [ ] **Step 1: Write the failing test `frontend/src/hooks/useStops.test.tsx`**

```tsx
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
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `npm test -- src/hooks/useStops.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Write `frontend/src/hooks/useStops.ts`**

```ts
"use client";

import { useCallback, useEffect, useState } from "react";
import { getStops } from "@/lib/api";
import type { StopDto } from "@/lib/types";

type Status = "loading" | "success" | "error";

export function useStops(): { stops: StopDto[]; status: Status; reload: () => void } {
  const [stops, setStops] = useState<StopDto[]>([]);
  const [status, setStatus] = useState<Status>("loading");

  const load = useCallback(() => {
    setStatus("loading");
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

  useEffect(() => {
    load();
  }, [load]);

  return { stops, status, reload: load };
}
```

- [ ] **Step 4: Run the test, verify it passes**

Run: `npm test -- src/hooks/useStops.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/hooks/useStops.ts frontend/src/hooks/useStops.test.tsx
git commit -m "feat: add useStops hook"
```

---

### Task 6: `useCoverage` hook

**Files:**
- Create: `frontend/src/hooks/useCoverage.ts`
- Create: `frontend/src/hooks/useCoverage.test.tsx`

**Interfaces:**
- Consumes: `getCoverage` from `@/lib/api`, `CoverageDto` from `@/lib/types`.
- Produces: `useCoverage(): { data: CoverageDto | null; status: "idle" | "loading" | "success" | "error"; load: () => void }`. Does **not** fetch on mount — `load()` is called when the coverage layer is toggled on (Task 8).

- [ ] **Step 1: Write the failing test `frontend/src/hooks/useCoverage.test.tsx`**

```tsx
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
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `npm test -- src/hooks/useCoverage.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Write `frontend/src/hooks/useCoverage.ts`**

```ts
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
```

- [ ] **Step 4: Run the test, verify it passes**

Run: `npm test -- src/hooks/useCoverage.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/hooks/useCoverage.ts frontend/src/hooks/useCoverage.test.tsx
git commit -m "feat: add useCoverage hook"
```

---

### Task 7: Leaflet map with clustered stop markers

**Files:**
- Create: `frontend/src/lib/leaflet-icons.ts`
- Create: `frontend/src/components/map/StopMarkers.tsx`
- Create: `frontend/src/components/map/MatatuMap.tsx`
- Create: `frontend/src/components/map/MapView.tsx` (the `next/dynamic` wrapper)
- Modify: `frontend/src/app/page.tsx` (render `MapView` full-bleed)
- Modify: `frontend/src/app/globals.css` (import `leaflet/dist/leaflet.css`; full-height html/body/#__next)

**Interfaces:**
- Consumes: `useStops` (Task 5); theme marker color vars (Task 4); `StopDto`.
- Produces:
  - `lib/leaflet-icons.ts`: `defaultIcon: L.Icon` (fixes the broken bundler icon paths, applied as `L.Marker.prototype.options.icon`); `coloredDivIcon(varName: string): L.DivIcon` returning a small colored circle marker, `varName` being one of the `--color-marker-*` token names.
  - `MatatuMap` (default export, `"use client"`): props `{ stops: StopDto[]; coverageSlot?: React.ReactNode; originStopId?: string | null; destinationStopId?: string | null }`. Renders `MapContainer` (center `[-1.2864, 36.8172]`, zoom 12), OSM `TileLayer`, `StopMarkers`, and `{coverageSlot}` as a child so a coverage layer can be injected later.
  - `StopMarkers` (`"use client"`): props `{ stops: StopDto[]; originStopId?: string | null; destinationStopId?: string | null; onSelect?: (stop: StopDto) => void }`. Clustered markers; origin/destination stops rendered with `coloredDivIcon("--color-marker-origin" | "--color-marker-destination")` and excluded from the cluster.
  - `MapView` (default export): `dynamic(() => import("./MatatuMap"), { ssr: false, loading: () => <MapSkeleton /> })`, re-exposing `MatatuMap`'s props.

- [ ] **Step 1: Install map dependencies**

```bash
cd frontend
npm install leaflet react-leaflet
npm install -D @types/leaflet
npm install react-leaflet-cluster
```
If `react-leaflet-cluster` reports a peer-dependency conflict with react-leaflet 5 / React 19, retry with `npm install react-leaflet-cluster --legacy-peer-deps`. If it still fails to render in Step 6, fall back: skip clustering, render plain `<Marker>` per stop, and add a `// TODO: clustering` note — do not block the task on it.

- [ ] **Step 2: Write `frontend/src/lib/leaflet-icons.ts`**

```ts
import L from "leaflet";
import iconRetina from "leaflet/dist/images/marker-icon-2x.png";
import icon from "leaflet/dist/images/marker-icon.png";
import shadow from "leaflet/dist/images/marker-shadow.png";

type MaybeStatic = string | { src: string };
const url = (v: MaybeStatic) => (typeof v === "string" ? v : v.src);

export const defaultIcon = L.icon({
  iconRetinaUrl: url(iconRetina),
  iconUrl: url(icon),
  shadowUrl: url(shadow),
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

L.Marker.prototype.options.icon = defaultIcon;

function cssVar(name: string): string {
  if (typeof window === "undefined") return "#2563eb";
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || "#2563eb";
}

export function coloredDivIcon(varName: string): L.DivIcon {
  const color = cssVar(varName);
  return L.divIcon({
    className: "matu-div-icon",
    html: `<span style="
      display:block;width:16px;height:16px;border-radius:9999px;
      background:${color};border:2px solid #fff;box-shadow:0 0 0 1px rgba(0,0,0,.25);"></span>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  });
}
```

- [ ] **Step 3: Update `globals.css`**

Add near the top, after the Tailwind import:
```css
@import "leaflet/dist/leaflet.css";

html,
body {
  height: 100%;
}
```

- [ ] **Step 4: Write `frontend/src/components/map/StopMarkers.tsx`**

```tsx
"use client";

import { Marker, Popup } from "react-leaflet";
import MarkerClusterGroup from "react-leaflet-cluster";
import { coloredDivIcon } from "@/lib/leaflet-icons";
import type { StopDto } from "@/lib/types";

interface Props {
  stops: StopDto[];
  originStopId?: string | null;
  destinationStopId?: string | null;
  onSelect?: (stop: StopDto) => void;
}

export default function StopMarkers({ stops, originStopId, destinationStopId, onSelect }: Props) {
  const highlighted = new Set([originStopId, destinationStopId].filter(Boolean) as string[]);
  const clustered = stops.filter((s) => !highlighted.has(s.stopId));
  const special = stops.filter((s) => highlighted.has(s.stopId));

  return (
    <>
      <MarkerClusterGroup chunkedLoading>
        {clustered.map((stop) => (
          <Marker
            key={stop.stopId}
            position={[stop.latitude, stop.longitude]}
            eventHandlers={onSelect ? { click: () => onSelect(stop) } : undefined}
          >
            <Popup>{stop.stopName}</Popup>
          </Marker>
        ))}
      </MarkerClusterGroup>
      {special.map((stop) => (
        <Marker
          key={stop.stopId}
          position={[stop.latitude, stop.longitude]}
          icon={coloredDivIcon(
            stop.stopId === originStopId ? "--color-marker-origin" : "--color-marker-destination",
          )}
        >
          <Popup>{stop.stopName}</Popup>
        </Marker>
      ))}
    </>
  );
}
```

- [ ] **Step 5: Write `frontend/src/components/map/MatatuMap.tsx` and `MapView.tsx`**

`MatatuMap.tsx`:
```tsx
"use client";

import { MapContainer, TileLayer } from "react-leaflet";
import type { ReactNode } from "react";
import StopMarkers from "./StopMarkers";
import "@/lib/leaflet-icons";
import type { StopDto } from "@/lib/types";

const NAIROBI: [number, number] = [-1.2864, 36.8172];

export interface MatatuMapProps {
  stops: StopDto[];
  coverageSlot?: ReactNode;
  originStopId?: string | null;
  destinationStopId?: string | null;
  onSelectStop?: (stop: StopDto) => void;
}

export default function MatatuMap({
  stops,
  coverageSlot,
  originStopId,
  destinationStopId,
  onSelectStop,
}: MatatuMapProps) {
  return (
    <MapContainer center={NAIROBI} zoom={12} className="h-full w-full" scrollWheelZoom>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <StopMarkers
        stops={stops}
        originStopId={originStopId}
        destinationStopId={destinationStopId}
        onSelect={onSelectStop}
      />
      {coverageSlot}
    </MapContainer>
  );
}
```

`MapView.tsx`:
```tsx
"use client";

import dynamic from "next/dynamic";
import type { MatatuMapProps } from "./MatatuMap";

function MapSkeleton() {
  return <div className="h-full w-full animate-pulse bg-muted" aria-label="Loading map" />;
}

const MatatuMap = dynamic(() => import("./MatatuMap"), { ssr: false, loading: MapSkeleton });

export default function MapView(props: MatatuMapProps) {
  return <MatatuMap {...props} />;
}
```

- [ ] **Step 6: Wire a minimal `page.tsx` and verify visually**

```tsx
"use client";

import MapView from "@/components/map/MapView";
import { useStops } from "@/hooks/useStops";

export default function Home() {
  const { stops, status } = useStops();
  return (
    <main className="h-screen w-screen">
      {status === "error" && (
        <div className="absolute left-1/2 top-4 z-[1000] -translate-x-1/2 rounded bg-red-600 px-3 py-1 text-white">
          Could not load stops.
        </div>
      )}
      <MapView stops={stops} />
    </main>
  );
}
```

Run `npm run dev` with the backend up. Open `http://localhost:3000`.
Expected: full-screen OSM map centered on Nairobi, clustered stop markers that expand on zoom, popups showing stop names.

- [ ] **Step 7: Lint, build, commit**

```bash
npm run lint
npm run build
git add frontend/src frontend/package.json frontend/package-lock.json
git commit -m "feat: render Leaflet map with clustered matatu stop markers"
```

---

### Task 8: Coverage layer

**Files:**
- Create: `frontend/src/components/map/CoverageLayer.tsx`
- Create: `frontend/src/components/map/CoverageStats.tsx`
- Create: `frontend/src/components/map/CoverageLayer.test.tsx`
- Modify: `frontend/src/app/page.tsx` (add a toggle button; pass `<CoverageLayer />` into `MapView`'s `coverageSlot`; render `<CoverageStats />` as an overlay)

**Interfaces:**
- Consumes: `useCoverage` (Task 6); `coloredDivIcon` (Task 7); shadcn `Button`, `Card`.
- Produces:
  - `CoverageLayer` (`"use client"`): props `{ data: CoverageDto }`. Renders `exampleIsolatedStops` with `coloredDivIcon("--color-marker-isolated")` and `worstServedStops` with `coloredDivIcon("--color-marker-poor")`, each `<Marker>` with a `<Popup>` of the stop name. Must be rendered **inside** `MapContainer` (passed via `coverageSlot`).
  - `CoverageStats` (`"use client"`): props `{ data: CoverageDto }`. A `Card` showing `totalStops`, `mainNetworkSize`, `isolatedClusterCount`. Rendered as a normal DOM overlay, **outside** the map.

- [ ] **Step 1: Write the failing test `frontend/src/components/map/CoverageLayer.test.tsx`**

Test `CoverageStats` (pure DOM, no Leaflet):

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import CoverageStats from "@/components/map/CoverageStats";

const data = {
  totalStops: 1200,
  mainNetworkSize: 1100,
  isolatedClusterCount: 7,
  exampleIsolatedStops: [],
  worstServedStops: [],
};

describe("CoverageStats", () => {
  it("shows the headline coverage numbers", () => {
    render(<CoverageStats data={data} />);
    expect(screen.getByText("1200")).toBeInTheDocument();
    expect(screen.getByText("1100")).toBeInTheDocument();
    expect(screen.getByText("7")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `npm test -- src/components/map/CoverageLayer.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Write `CoverageStats.tsx`**

```tsx
"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { CoverageDto } from "@/lib/types";

export default function CoverageStats({ data }: { data: CoverageDto }) {
  const rows: [string, number][] = [
    ["Total stops", data.totalStops],
    ["On main network", data.mainNetworkSize],
    ["Isolated clusters", data.isolatedClusterCount],
  ];
  return (
    <Card className="w-56">
      <CardHeader>
        <CardTitle className="text-sm">Network coverage</CardTitle>
      </CardHeader>
      <CardContent className="space-y-1 text-sm">
        {rows.map(([label, value]) => (
          <div key={label} className="flex justify-between">
            <span className="text-muted-foreground">{label}</span>
            <span className="font-medium">{value}</span>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 4: Write `CoverageLayer.tsx`**

```tsx
"use client";

import { Marker, Popup } from "react-leaflet";
import { coloredDivIcon } from "@/lib/leaflet-icons";
import type { CoverageDto, StopDto } from "@/lib/types";

function markers(stops: StopDto[], varName: string) {
  return stops.map((stop) => (
    <Marker key={`${varName}-${stop.stopId}`} position={[stop.latitude, stop.longitude]} icon={coloredDivIcon(varName)}>
      <Popup>{stop.stopName}</Popup>
    </Marker>
  ));
}

export default function CoverageLayer({ data }: { data: CoverageDto }) {
  return (
    <>
      {markers(data.exampleIsolatedStops, "--color-marker-isolated")}
      {markers(data.worstServedStops, "--color-marker-poor")}
    </>
  );
}
```

- [ ] **Step 5: Run the test, verify it passes**

Run: `npm test -- src/components/map/CoverageLayer.test.tsx`
Expected: PASS.

- [ ] **Step 6: Wire into `page.tsx`**

Add a `showCoverage` state, a shadcn `Button` toggle (top-left overlay, `z-[1000]`), call `useCoverage().load()` when toggled on, pass `coverage.data ? <CoverageLayer data={coverage.data} /> : undefined` to `MapView`'s `coverageSlot`, and render `<CoverageStats data={coverage.data} />` in an overlay when `coverage.status === "success"`. Show a small inline error if `coverage.status === "error"`.

- [ ] **Step 7: Verify visually**

`npm run dev`, backend up. Toggle the coverage button.
Expected: red (isolated) and amber (poorly-served) circle markers appear; the stats card shows real counts; toggling off removes them.

- [ ] **Step 8: Lint, build, commit**

```bash
npm run lint
npm run build
git add frontend/src
git commit -m "feat: add coverage-gap map layer and stats card"
```

---

### Task 9: Route planner

**Files:**
- Create: `frontend/src/components/map/StopCombobox.tsx`
- Create: `frontend/src/components/map/RoutePlanner.tsx`
- Create: `frontend/src/components/map/RoutePlanner.test.tsx`
- Modify: `frontend/src/app/page.tsx` (render `RoutePlanner` overlay; lift origin/destination stop ids to page state; pass them to `MapView`)

**Interfaces:**
- Consumes: `searchStops`, `suggestRoute` from `@/lib/api`; `RouteAdviceDto`, `StopDto`; shadcn `Command`, `Button`, `Card`.
- Produces:
  - `StopCombobox` (`"use client"`): props `{ label: string; value: StopDto | null; onChange: (stop: StopDto | null) => void }`. Debounced (300 ms) calls to `searchStops`; renders results in a `Command` list; selecting one calls `onChange`.
  - `RoutePlanner` (`"use client"`): props `{ onSelectionChange: (origin: StopDto | null, destination: StopDto | null) => void }`. Two `StopCombobox`es + a "Find route" `Button` (disabled until both chosen). On click calls `suggestRoute(origin.stopId, destination.stopId)`; renders a result `Card`: if `routeFound` — `routeNamesUsed` joined as "Board: …", the ordered `stopNamesInOrder` list, `estimatedRideMinutes` min, `transferCount` transfers; if not — the text "No route found between these stops." Calls `onSelectionChange` whenever either box changes.

- [ ] **Step 1: Write the failing test `frontend/src/components/map/RoutePlanner.test.tsx`**

```tsx
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RoutePlanner from "@/components/map/RoutePlanner";
import * as api from "@/lib/api";

const kencom = { stopId: "K", stopName: "Kencom", latitude: -1.28, longitude: 36.82 };
const westlands = { stopId: "W", stopName: "Westlands", latitude: -1.26, longitude: 36.8 };

beforeEach(() => {
  vi.restoreAllMocks();
  vi.spyOn(api, "searchStops").mockImplementation(async (q: string) =>
    [kencom, westlands].filter((s) => s.stopName.toLowerCase().includes(q.toLowerCase())),
  );
});

describe("RoutePlanner", () => {
  it("keeps 'Find route' disabled until both stops are chosen", async () => {
    render(<RoutePlanner onSelectionChange={() => {}} />);
    expect(screen.getByRole("button", { name: /find route/i })).toBeDisabled();
  });

  it("renders a not-found message when routeFound is false", async () => {
    vi.spyOn(api, "suggestRoute").mockResolvedValue({
      routeFound: false, stopNamesInOrder: [], routeNamesUsed: [], estimatedRideMinutes: 0, transferCount: 0,
    });
    render(<RoutePlanner onSelectionChange={() => {}} />);
    // Test-only helper hooks: the component exposes data-testid inputs for typing.
    await userEvent.type(screen.getByTestId("origin-input"), "Kencom");
    await userEvent.click(await screen.findByText("Kencom"));
    await userEvent.type(screen.getByTestId("destination-input"), "Westlands");
    await userEvent.click(await screen.findByText("Westlands"));
    await userEvent.click(screen.getByRole("button", { name: /find route/i }));
    expect(await screen.findByText(/no route found between these stops/i)).toBeInTheDocument();
  });

  it("renders route details when a route is found", async () => {
    vi.spyOn(api, "suggestRoute").mockResolvedValue({
      routeFound: true,
      stopNamesInOrder: ["Kencom", "Museum Hill", "Westlands"],
      routeNamesUsed: ["46"],
      estimatedRideMinutes: 24,
      transferCount: 0,
    });
    render(<RoutePlanner onSelectionChange={() => {}} />);
    await userEvent.type(screen.getByTestId("origin-input"), "Kencom");
    await userEvent.click(await screen.findByText("Kencom"));
    await userEvent.type(screen.getByTestId("destination-input"), "Westlands");
    await userEvent.click(await screen.findByText("Westlands"));
    await userEvent.click(screen.getByRole("button", { name: /find route/i }));
    expect(await screen.findByText(/46/)).toBeInTheDocument();
    expect(screen.getByText(/24/)).toBeInTheDocument();
    expect(screen.getByText("Museum Hill")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `npm test -- src/components/map/RoutePlanner.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Write `StopCombobox.tsx`**

```tsx
"use client";

import { useEffect, useRef, useState } from "react";
import { Command, CommandEmpty, CommandInput, CommandItem, CommandList } from "@/components/ui/command";
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
    if (query.trim().length < 2) {
      setResults([]);
      return;
    }
    timer.current = setTimeout(() => {
      searchStops(query).then(setResults).catch(() => setResults([]));
    }, 300);
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
```

- [ ] **Step 4: Write `RoutePlanner.tsx`**

```tsx
"use client";

import { useEffect, useState } from "react";
import StopCombobox from "./StopCombobox";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { suggestRoute } from "@/lib/api";
import type { RouteAdviceDto, StopDto } from "@/lib/types";

interface Props {
  onSelectionChange: (origin: StopDto | null, destination: StopDto | null) => void;
}

export default function RoutePlanner({ onSelectionChange }: Props) {
  const [origin, setOrigin] = useState<StopDto | null>(null);
  const [destination, setDestination] = useState<StopDto | null>(null);
  const [result, setResult] = useState<RouteAdviceDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    onSelectionChange(origin, destination);
  }, [origin, destination, onSelectionChange]);

  async function findRoute() {
    if (!origin || !destination) return;
    setLoading(true);
    setError(false);
    setResult(null);
    try {
      setResult(await suggestRoute(origin.stopId, destination.stopId));
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="w-72">
      <CardHeader>
        <CardTitle className="text-sm">Plan a route</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <StopCombobox label="Origin" testId="origin-input" value={origin} onChange={setOrigin} />
        <StopCombobox label="Destination" testId="destination-input" value={destination} onChange={setDestination} />
        <Button className="w-full" disabled={!origin || !destination || loading} onClick={findRoute}>
          {loading ? "Finding…" : "Find route"}
        </Button>

        {error && <p className="text-sm text-red-600">Could not fetch a route. Try again.</p>}

        {result && !result.routeFound && (
          <p className="text-sm text-muted-foreground">No route found between these stops.</p>
        )}

        {result && result.routeFound && (
          <div className="space-y-1 text-sm">
            <p><span className="font-medium">Board:</span> {result.routeNamesUsed.join(" → ")}</p>
            <p>{result.estimatedRideMinutes} min · {result.transferCount} transfer{result.transferCount === 1 ? "" : "s"}</p>
            <ol className="list-decimal pl-5 text-muted-foreground">
              {result.stopNamesInOrder.map((name, i) => (
                <li key={`${name}-${i}`}>{name}</li>
              ))}
            </ol>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 5: Run the test, verify it passes**

Run: `npm test -- src/components/map/RoutePlanner.test.tsx`
Expected: PASS. If `CommandInput` does not forward `data-testid`, add it explicitly in the generated `src/components/ui/command.tsx` `CommandInput` (spread `...props` onto the underlying input) — a one-line fix in the shadcn file.

- [ ] **Step 6: Wire into `page.tsx`**

Render `<RoutePlanner onSelectionChange={(o, d) => { setOriginId(o?.stopId ?? null); setDestId(d?.stopId ?? null); }} />` in the top-left overlay stack (below the coverage toggle), and pass `originStopId={originId}` / `destinationStopId={destId}` to `MapView`.

- [ ] **Step 7: Verify visually**

`npm run dev`, backend up. Search a real origin and destination, click "Find route".
Expected: result card with routes/stops/time; origin and destination markers change color on the map; an unroutable pair shows "No route found between these stops."

- [ ] **Step 8: Lint, build, commit**

```bash
npm run lint
npm run build
git add frontend/src
git commit -m "feat: add route planner with stop search comboboxes"
```

---

### Task 10: Chat panel

**Files:**
- Create: `frontend/src/components/chat/ChatInput.tsx`
- Create: `frontend/src/components/chat/MessageList.tsx`
- Create: `frontend/src/components/chat/ChatPanel.tsx`
- Create: `frontend/src/components/chat/ChatPanel.test.tsx`

**Interfaces:**
- Consumes: `sendChatMessage` from `@/lib/api`; `ChatMessage` from `@/lib/types`; shadcn `Input`, `Button`, `ScrollArea`.
- Produces:
  - `ChatInput` (`"use client"`): props `{ disabled: boolean; onSend: (text: string) => void }`. An `Input` + send `Button`; Enter or click sends non-empty trimmed text and clears the field.
  - `MessageList` (`"use client"`): props `{ messages: ChatMessage[]; pending: boolean }`. Renders each message as a bubble (user right-aligned, assistant left, `role: "error"` as a red left bubble); when `pending`, a "typing…" indicator bubble.
  - `ChatPanel` (default export, `"use client"`): no required props (optional `className`). Owns `ChatMessage[]` state; on send appends `{ role: "user", content }`, sets `pending`, calls `sendChatMessage`, appends `{ role: "assistant", content: reply }` or `{ role: "error", content: "Something went wrong. Try again." }`. History is in-memory only.

- [ ] **Step 1: Write the failing test `frontend/src/components/chat/ChatPanel.test.tsx`**

```tsx
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ChatPanel from "@/components/chat/ChatPanel";
import * as api from "@/lib/api";

beforeEach(() => {
  vi.restoreAllMocks();
});

describe("ChatPanel", () => {
  it("shows the user message then the assistant reply", async () => {
    vi.spyOn(api, "sendChatMessage").mockResolvedValue({ reply: "Take route 46." });
    render(<ChatPanel />);
    await userEvent.type(screen.getByRole("textbox"), "How do I get to town?");
    await userEvent.click(screen.getByRole("button", { name: /send/i }));
    expect(screen.getByText("How do I get to town?")).toBeInTheDocument();
    expect(await screen.findByText("Take route 46.")).toBeInTheDocument();
  });

  it("shows an error bubble when the request fails", async () => {
    vi.spyOn(api, "sendChatMessage").mockRejectedValue(new Error("boom"));
    render(<ChatPanel />);
    await userEvent.type(screen.getByRole("textbox"), "hi");
    await userEvent.click(screen.getByRole("button", { name: /send/i }));
    expect(await screen.findByText(/something went wrong/i)).toBeInTheDocument();
  });

  it("does not send empty input", async () => {
    const spy = vi.spyOn(api, "sendChatMessage").mockResolvedValue({ reply: "x" });
    render(<ChatPanel />);
    await userEvent.click(screen.getByRole("button", { name: /send/i }));
    expect(spy).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `npm test -- src/components/chat/ChatPanel.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Write `ChatInput.tsx`**

```tsx
"use client";

import { useState, type FormEvent } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export default function ChatInput({ disabled, onSend }: { disabled: boolean; onSend: (text: string) => void }) {
  const [text, setText] = useState("");

  function submit(e: FormEvent) {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText("");
  }

  return (
    <form onSubmit={submit} className="flex gap-2 border-t p-2">
      <Input
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Ask about a route…"
        disabled={disabled}
      />
      <Button type="submit" disabled={disabled}>Send</Button>
    </form>
  );
}
```

- [ ] **Step 4: Write `MessageList.tsx`**

```tsx
"use client";

import { ScrollArea } from "@/components/ui/scroll-area";
import type { ChatMessage } from "@/lib/types";

const bubble: Record<ChatMessage["role"], string> = {
  user: "ml-auto bg-primary text-primary-foreground",
  assistant: "mr-auto bg-muted",
  error: "mr-auto bg-red-100 text-red-800",
};

export default function MessageList({ messages, pending }: { messages: ChatMessage[]; pending: boolean }) {
  return (
    <ScrollArea className="flex-1 p-3">
      <div className="flex flex-col gap-2">
        {messages.map((m, i) => (
          <div key={i} className={`max-w-[80%] rounded-lg px-3 py-2 text-sm ${bubble[m.role]}`}>
            {m.content}
          </div>
        ))}
        {pending && (
          <div className="mr-auto max-w-[80%] rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
            typing…
          </div>
        )}
      </div>
    </ScrollArea>
  );
}
```

- [ ] **Step 5: Write `ChatPanel.tsx`**

```tsx
"use client";

import { useState } from "react";
import MessageList from "./MessageList";
import ChatInput from "./ChatInput";
import { sendChatMessage } from "@/lib/api";
import type { ChatMessage } from "@/lib/types";

export default function ChatPanel({ className }: { className?: string }) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [pending, setPending] = useState(false);

  async function handleSend(text: string) {
    setMessages((prev) => [...prev, { role: "user", content: text }]);
    setPending(true);
    try {
      const { reply } = await sendChatMessage(text);
      setMessages((prev) => [...prev, { role: "assistant", content: reply }]);
    } catch {
      setMessages((prev) => [...prev, { role: "error", content: "Something went wrong. Try again." }]);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className={`flex h-full flex-col ${className ?? ""}`}>
      <header className="border-b p-3 font-semibold">MatuConnect assistant</header>
      <MessageList messages={messages} pending={pending} />
      <ChatInput disabled={pending} onSend={handleSend} />
    </div>
  );
}
```

- [ ] **Step 6: Optionally regenerate bubbles/header with 21st.dev**

Use the `magic` MCP (`mcp__magic__generate`) to produce a more polished chat thread + input against the Task 4 tokens. Any replacement **must** keep: `role="textbox"` input, a button named "Send", user/assistant/error bubble rendering, and the `typing…` pending indicator — so `ChatPanel.test.tsx` still passes. Re-run the test after adapting.

- [ ] **Step 7: Run the test, verify it passes**

Run: `npm test -- src/components/chat/ChatPanel.test.tsx`
Expected: PASS.

- [ ] **Step 8: Lint, build, commit**

```bash
npm run lint
npm run build
git add frontend/src
git commit -m "feat: add chat panel calling POST /api/chat"
```

---

### Task 11: Page assembly — desktop split + mobile drawer + overlays

**Files:**
- Modify: `frontend/src/app/page.tsx` (final layout)
- Modify: `frontend/src/app/layout.tsx` (remove default body padding/margins; ensure full-height)
- Create: `frontend/src/components/MapOverlays.tsx` (the top-left overlay stack: coverage toggle + `RoutePlanner` + `CoverageStats`)
- Create: `frontend/src/components/ChatDock.tsx` (desktop fixed panel; mobile `Sheet` triggered by a floating button)

**Interfaces:**
- Consumes: everything from Tasks 5-10.
- Produces: the finished single-page app. `page.tsx` owns `originId`/`destId`/`showCoverage` state; `MapOverlays` receives handlers; `ChatDock` wraps `ChatPanel`.

- [ ] **Step 1: Write `ChatDock.tsx`**

```tsx
"use client";

import { useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import ChatPanel from "@/components/chat/ChatPanel";

export default function ChatDock() {
  const [open, setOpen] = useState(false);
  return (
    <>
      {/* Desktop: fixed right panel */}
      <aside className="hidden h-full w-[380px] shrink-0 border-l bg-background lg:block">
        <ChatPanel />
      </aside>

      {/* Mobile: FAB + sheet */}
      <div className="lg:hidden">
        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger asChild>
            <Button className="fixed bottom-4 right-4 z-[1000] rounded-full shadow-lg">Chat</Button>
          </SheetTrigger>
          <SheetContent side="right" className="w-full p-0 sm:w-[380px]">
            <ChatPanel />
          </SheetContent>
        </Sheet>
      </div>
    </>
  );
}
```

- [ ] **Step 2: Write `MapOverlays.tsx`**

```tsx
"use client";

import { Button } from "@/components/ui/button";
import RoutePlanner from "@/components/map/RoutePlanner";
import CoverageStats from "@/components/map/CoverageStats";
import type { CoverageDto, StopDto } from "@/lib/types";

interface Props {
  showCoverage: boolean;
  coverageData: CoverageDto | null;
  coverageError: boolean;
  onToggleCoverage: () => void;
  onSelectionChange: (origin: StopDto | null, destination: StopDto | null) => void;
}

export default function MapOverlays({
  showCoverage,
  coverageData,
  coverageError,
  onToggleCoverage,
  onSelectionChange,
}: Props) {
  return (
    <div className="pointer-events-none absolute left-3 top-3 z-[1000] flex max-h-[calc(100%-1.5rem)] flex-col gap-2 overflow-y-auto">
      <div className="pointer-events-auto">
        <Button variant={showCoverage ? "default" : "secondary"} onClick={onToggleCoverage}>
          {showCoverage ? "Hide coverage gaps" : "Show coverage gaps"}
        </Button>
      </div>
      <div className="pointer-events-auto">
        <RoutePlanner onSelectionChange={onSelectionChange} />
      </div>
      {showCoverage && coverageError && (
        <div className="pointer-events-auto rounded bg-red-600 px-3 py-1 text-sm text-white">
          Could not load coverage data.
        </div>
      )}
      {showCoverage && coverageData && (
        <div className="pointer-events-auto">
          <CoverageStats data={coverageData} />
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Write the final `page.tsx`**

```tsx
"use client";

import { useCallback, useEffect, useState } from "react";
import MapView from "@/components/map/MapView";
import CoverageLayer from "@/components/map/CoverageLayer";
import MapOverlays from "@/components/MapOverlays";
import ChatDock from "@/components/ChatDock";
import { useStops } from "@/hooks/useStops";
import { useCoverage } from "@/hooks/useCoverage";
import type { StopDto } from "@/lib/types";

export default function Home() {
  const { stops, status: stopsStatus, reload } = useStops();
  const coverage = useCoverage();
  const [showCoverage, setShowCoverage] = useState(false);
  const [originId, setOriginId] = useState<string | null>(null);
  const [destId, setDestId] = useState<string | null>(null);

  useEffect(() => {
    if (showCoverage && coverage.status === "idle") coverage.load();
  }, [showCoverage, coverage]);

  const onSelectionChange = useCallback((origin: StopDto | null, destination: StopDto | null) => {
    setOriginId(origin?.stopId ?? null);
    setDestId(destination?.stopId ?? null);
  }, []);

  return (
    <main className="flex h-screen w-screen overflow-hidden">
      <div className="relative flex-1">
        {stopsStatus === "error" && (
          <div className="absolute left-1/2 top-3 z-[1000] -translate-x-1/2 rounded bg-red-600 px-3 py-1 text-sm text-white">
            Could not load stops.{" "}
            <button className="underline" onClick={reload}>Retry</button>
          </div>
        )}
        <MapOverlays
          showCoverage={showCoverage}
          coverageData={showCoverage ? coverage.data : null}
          coverageError={coverage.status === "error"}
          onToggleCoverage={() => setShowCoverage((v) => !v)}
          onSelectionChange={onSelectionChange}
        />
        <MapView
          stops={stops}
          originStopId={originId}
          destinationStopId={destId}
          coverageSlot={showCoverage && coverage.data ? <CoverageLayer data={coverage.data} /> : undefined}
        />
      </div>
      <ChatDock />
    </main>
  );
}
```

- [ ] **Step 4: Full test + lint + build**

```bash
cd frontend
npm test
npm run lint
npm run build
```
Expected: all green.

- [ ] **Step 5: Manual end-to-end check (backend running)**

- Desktop width: map left, chat panel right, overlays top-left. Plan a route; toggle coverage; send a chat message and get a real agent reply.
- Mobile width (devtools ~390px): full-screen map, "Chat" FAB opens the sheet, overlays scroll.

- [ ] **Step 6: Commit**

```bash
cd ..
git add frontend/src
git commit -m "feat: assemble split-view page with mobile chat drawer"
```

---

### Task 12: Error/loading polish and frontend README

**Files:**
- Modify: `frontend/src/components/map/MapView.tsx` (skeleton already present — confirm it reads well)
- Modify: `frontend/src/app/page.tsx` (a subtle "loading stops…" indicator while `stopsStatus === "loading"`)
- Create: `frontend/README.md`

**Interfaces:** none new.

- [ ] **Step 1: Add a stops-loading hint**

In `page.tsx`, when `stopsStatus === "loading"`, render a small non-blocking pill (top-center, `z-[1000]`): "Loading stops…". Remove it on success/error.

- [ ] **Step 2: Write `frontend/README.md`**

````markdown
# MatuConnect Frontend

Next.js 15 + Leaflet map and chat UI for the MatuConnect Nairobi matatu
route advisor. Talks to the Spring Boot backend's REST API.

## Prerequisites

- Node 22+
- The MatuConnect backend running on `http://localhost:8080`
  (`./mvnw spring-boot:run` from the repo root)

## Setup

```bash
cd frontend
npm install
cp .env.local.example .env.local   # optional: change BACKEND_URL
npm run dev
```

Open http://localhost:3000.

## How API calls work

The browser only calls relative `/api/*` URLs. `next.config.ts` rewrites
those to `${BACKEND_URL:-http://localhost:8080}/api/*`, so there is no
CORS configuration on either side.

## Scripts

| Command | Purpose |
|---|---|
| `npm run dev` | Dev server |
| `npm run build` | Production build |
| `npm test` | Vitest unit tests |
| `npm run lint` | ESLint |

## Structure

- `src/app/page.tsx` — the single split-view page
- `src/components/map/*` — Leaflet map, stop markers, route planner, coverage layer
- `src/components/chat/*` — chat panel
- `src/lib/api.ts` — typed API client (all endpoints)
- `src/hooks/*` — `useStops`, `useCoverage`

## Known limitations

- The route planner is textual — `/api/routes/suggest` returns stop
  names only, no coordinates, so no route polyline is drawn.
- Chat has no server-side memory; each message is independent.
````

- [ ] **Step 3: Test, lint, build**

```bash
cd frontend
npm test
npm run lint
npm run build
```
Expected: all green.

- [ ] **Step 4: Commit**

```bash
cd ..
git add frontend
git commit -m "chore: loading states polish and frontend README"
```

---

## Self-Review

**1. Spec coverage**

| Spec item | Task |
|---|---|
| `ChatController` / `POST /api/chat` | 1 |
| Next.js app in `frontend/`, App Router, TS | 2 |
| CORS via `next.config.ts` rewrites + `BACKEND_URL` | 2 |
| `lib/types.ts`, `lib/api.ts` with `ApiError` | 3 |
| Vitest + RTL setup | 3 |
| `ui-ux-pro-max` tokens, shadcn primitives, 21st.dev | 4 (tokens/primitives), 10 step 6 (21st.dev) |
| Spec updated with chosen style | 4 step 6 |
| `useStops` | 5 |
| `useCoverage` | 6 |
| `leaflet-icons.ts` bundler fix + colored icons | 7 |
| `MatatuMap` dynamic `ssr:false` + skeleton | 7 |
| `StopMarkers` clustered, origin/dest highlight | 7 |
| `CoverageLayer` + stats readout | 8 |
| `RoutePlanner` + `/api/stops/search` comboboxes + result card + `routeFound:false` text | 9 |
| `ChatPanel` / `MessageList` / `ChatInput`, error bubble, no persistence | 10 |
| Desktop split + mobile `Sheet` drawer + overlay controls | 11 |
| Error-handling table (per-panel inline errors, retry, skeleton) | 7, 8, 9, 10, 11, 12 |
| `@WebMvcTest(ChatController.class)` | 1 |
| Frontend unit tests (api, RoutePlanner, MessageList/ChatPanel) | 3, 9, 10 |
| `frontend/README.md` | 12 |

No uncovered spec items. Out-of-scope items (route polyline, chat memory, auth) are intentionally absent and noted in the README.

**2. Placeholder scan**

No "TBD"/"TODO" left as instructions. The one deliberate deferral — the concrete palette/font values — is produced by the `ui-ux-pro-max` run in Task 4 Step 1 and written back in Step 6; the CSS in Task 4 Step 2 and Task 7 uses named `--color-marker-*` / `--color-primary` tokens so later tasks do not depend on specific hex values. The `react-leaflet-cluster` fallback in Task 7 Step 1 is a concrete instruction, not a placeholder.

**3. Type consistency**

- `StopDto` / `RouteAdviceDto` / `CoverageDto` / `ChatResponse` / `ChatMessage` defined once in Task 3, imported everywhere.
- `useStops` → `{ stops, status, reload }`; consumed in Task 7 Step 6 and Task 11 Step 3 with those exact names.
- `useCoverage` → `{ data, status, load }`; consumed in Task 8 Step 6 and Task 11 with those names (`status` values `idle|loading|success|error`).
- `coloredDivIcon(varName: string)` defined in Task 7, called in Task 7 and Task 8 with `--color-marker-*` strings.
- `MatatuMapProps` (`stops`, `coverageSlot`, `originStopId`, `destinationStopId`, `onSelectStop`) defined in Task 7, used by `MapView` and `page.tsx` consistently.
- `RoutePlanner` prop `onSelectionChange(origin, destination)` defined Task 9, consumed Task 11.
- `ChatPanel` optional `className` only — Task 10 and Task 11 (`ChatDock`) agree.
- `sendChatMessage` / `searchStops` / `suggestRoute` / `getStops` / `getCoverage` signatures identical between Task 3 definition and all mocked usages in tests.

No mismatches found.
