# MatuConnect Next.js Frontend — Design

**Date:** 2026-09-07
**Status:** Approved (design), pending spec review
**Branch:** `feat/nextjs-frontend`

## Purpose

Build the web frontend for MatuConnect: an interactive Leaflet map of the
Nairobi matatu network plus a chat interface backed by the existing
Spring AI route-advisory agent. The Spring Boot backend (entities,
repositories, GTFS ingestion, JGraphT graph analysis, AI agent, RAG,
REST API) is already built and runs at `http://localhost:8080`.

## Scope

In scope:

- Next.js app in `matuconnect/frontend/` (same repo, subdirectory).
- Single split-view page: map + chat.
- Stop markers, stop search, point-to-point route planner, coverage-gap
  overlay.
- Chat panel calling the agent.
- One backend addition: `ChatController` exposing `POST /api/chat`
  (the DTO records already exist; the controller does not).

Out of scope:

- Route polyline drawing on the map — `RouteAdviceDto` returns stop
  *names* only, no coordinates. Deferred; would need a new backend
  endpoint exposing `ShapePoint` / stop coordinates for a route.
- Server-side chat memory / sessions — each `/api/chat` call is
  independent. Chat history is client-side only.
- Authentication, user accounts, persistence of user state.
- Deployment configuration beyond a documented `BACKEND_URL` env var.

## Backend contract (verified from source)

| Method & path | Query / body | Response shape |
|---|---|---|
| `GET /api/stops` | — | `StopDto[]` |
| `GET /api/stops/search` | `query` (string) | `StopDto[]` |
| `GET /api/routes/suggest` | `originStopId`, `destinationStopId` | `RouteAdviceDto` |
| `GET /api/coverage` | — | `CoverageDto` |
| `POST /api/chat` | `ChatRequest` | `ChatResponse` | **(to be added)** |

```
StopDto        { stopId: string, stopName: string, latitude: number, longitude: number }

RouteAdviceDto { routeFound: boolean,
                 stopNamesInOrder: string[],
                 routeNamesUsed: string[],
                 estimatedRideMinutes: number,
                 transferCount: number }

CoverageDto    { totalStops: number,
                 mainNetworkSize: number,
                 isolatedClusterCount: number,
                 exampleIsolatedStops: StopDto[],
                 worstServedStops: StopDto[] }

ChatRequest    { message: string }
ChatResponse   { reply: string }
```

Behavioural notes from the backend source:

- `/api/routes/suggest` returns HTTP 200 with `routeFound: false` when no
  path exists — this is a normal outcome, not an error. Frontend renders
  a plain "no route found" message.
- `/api/coverage` caps example stop lists at 20 each.
- The `ChatClient` bean is fully wired (system prompt, route-advisory
  tools, RAG advisor). `ChatController` only needs to call it.

## Backend change: `ChatController`

New file: `src/main/java/com/matuconnect/controller/ChatController.java`,
same package as the existing package-private `ChatRequest` / `ChatResponse`
records in `ChatMessages.java`.

```java
package com.matuconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

- Blocking call — no streaming. Frontend shows a typing indicator.
- No CORS config added to the backend; the frontend proxies (see below).
- Matches the class visibility style of the other controllers in the
  package.

## Frontend architecture

### Stack

- Next.js 15, App Router, TypeScript, npm.
- Tailwind CSS v4.
- shadcn/ui primitives: `button`, `input`, `card`, `sheet`,
  `scroll-area`, `command` (for the stop-search combobox). Added via
  `npx shadcn@latest add` — the shadcn MCP server is unavailable, the
  CLI path is used instead.
- Map: `react-leaflet` + `leaflet` + `react-leaflet-cluster`.
- Data access: typed `fetch` wrappers + React hooks. No data-fetching
  library — three GETs and one POST do not justify one.

### CORS strategy — Next.js rewrites proxy

`next.config.ts`:

```ts
const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig = {
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};
```

The browser only ever calls relative `/api/...`, so it is same-origin and
no CORS headers are needed. Works in dev and prod; the backend stays
unchanged. `BACKEND_URL` documented in `.env.local.example`.

### Directory layout

```
matuconnect/frontend/
  next.config.ts
  tsconfig.json
  components.json               # shadcn config
  .env.local.example            # BACKEND_URL=http://localhost:8080
  package.json
  src/
    app/
      layout.tsx
      page.tsx                  # split view: map + chat
      globals.css              # Tailwind + leaflet CSS import
    components/
      map/
        MatatuMap.tsx           # dynamic(ssr:false) wrapper + MapContainer
        StopMarkers.tsx         # clustered stop markers
        CoverageLayer.tsx       # isolated / worst-served markers + stats
        RoutePlanner.tsx        # origin/dest comboboxes + result card
      chat/
        ChatPanel.tsx           # thread + input, desktop panel / mobile drawer
        MessageList.tsx
        ChatInput.tsx
      ui/                       # shadcn-generated primitives
    lib/
      api.ts                    # typed fetch wrappers, throws ApiError on non-2xx
      types.ts                  # StopDto, RouteAdviceDto, CoverageDto, ChatResponse, ChatMessage
      leaflet-icons.ts          # marker-icon bundler fix + colored icon variants
    hooks/
      useStops.ts               # fetch /api/stops once, cache in state
      useCoverage.ts            # fetch /api/coverage on demand
```

### Page layout

- Desktop (`>= lg`): map fills the main area; chat is a fixed-width right
  panel (~380px).
- Mobile: map is full-screen; chat is a shadcn `Sheet` drawer opened by a
  floating action button.
- Map overlay controls (top-left): `RoutePlanner` card, coverage toggle
  button. Positioned with Leaflet-independent absolutely-positioned divs
  over the map container so they are normal React/shadcn components.

### Component responsibilities

**`MatatuMap`** — owns the `MapContainer`, OSM `TileLayer`, initial view
(`[-1.2864, 36.8172]`, zoom 12). Renders `StopMarkers` always;
`CoverageLayer` when the toggle is on; passes route-planner selection
down so origin/destination markers can be emphasised. Loaded via
`next/dynamic` with `ssr: false`.

**`StopMarkers`** — takes the stops array, renders a clustered marker
layer, popup shows `stopName`. Click optionally sets route-planner
origin/destination.

**`RoutePlanner`** — two `command`-based comboboxes querying
`/api/stops/search?query=`. On both selected + "Find route", calls
`GET /api/routes/suggest`. Renders a result `Card`: `routeNamesUsed`
("routes to board"), `stopNamesInOrder` (ordered list), `estimatedRideMinutes`,
`transferCount`. `routeFound: false` → "No route found between these
stops." Selecting origin/destination highlights those markers on the map.

**`CoverageLayer`** — on toggle, `useCoverage` fetches `/api/coverage`.
Renders `exampleIsolatedStops` and `worstServedStops` as two visually
distinct colored marker sets, plus a small stats readout
(`totalStops`, `mainNetworkSize`, `isolatedClusterCount`).

**`ChatPanel`** — holds `ChatMessage[]` in state (`{ role, content }`).
`ChatInput` submit → append user message → `POST /api/chat` → append
assistant reply. Typing indicator while awaiting. Error → an error
bubble in the thread with a retry affordance. No history persistence.

**`lib/api.ts`** — one function per endpoint, all returning typed
promises. Non-2xx throws `ApiError { status, message }`. All URLs
relative (`/api/...`).

## Visual design process

The look is decided before components are built, in this order:

1. **`ui-ux-pro-max` skill** — choose style direction, product palette +
   reasoning profile, font pairing, and applicable UX guidelines for a
   map-forward, data-dense, mobile-first transit tool. Output: design
   tokens (color, type scale, spacing, radius) written into
   `globals.css` / Tailwind theme, and recorded in this spec once known.
2. **shadcn primitives** — add the base components listed above, themed
   to the tokens from step 1.
3. **21st.dev (`magic` MCP)** — generate the composite pieces
   (`ChatPanel`, `RoutePlanner` card, coverage stats readout, mobile
   chat FAB + drawer) against those tokens, then adapt.
4. **Hand-assembly** — map overlay positioning and wiring.

> **Chosen style / palette / fonts** (from `ui-ux-pro-max`, applied in
> `src/app/globals.css` + `src/app/layout.tsx`):
>
> - **Style direction:** "Data-Dense Dashboard" — blue-data + amber-highlight;
>   multiple overlays/widgets, minimal padding, maximum data visibility,
>   light + dark support.
> - **Palette (light):** primary `#1E40AF`, primary-fg `#FFFFFF`,
>   secondary/muted `#E9EEF6`, muted-fg `#475569`, background `#F8FAFC`,
>   foreground `#1E3A8A`, card `#FFFFFF`, border/input/accent `#DBEAFE`,
>   ring `#1E40AF`, destructive `#DC2626`, brand-accent (CTA) `#D97706`.
> - **Palette (dark):** primary `#3B82F6`, primary-fg `#0F172A`,
>   background `#0F172A`, foreground `#F8FAFC`, card `#1B2336`,
>   secondary/accent `#1E293B`, muted `#272F42`, muted-fg `#94A3B8`,
>   border/input `#334155`, ring `#3B82F6`, destructive `#EF4444`,
>   brand-accent `#F59E0B`.
> - **Marker tokens** (`--color-marker-*`, consumed by `lib/leaflet-icons.ts`):
>   stop `#2563EB`, isolated `#DC2626`, poor `#D97706`, origin `#16A34A`,
>   destination `#7C3AED` (light) / `#3B82F6`, `#F87171`, `#FBBF24`,
>   `#4ADE80`, `#A78BFA` (dark).
> - **Font pairing:** Fira Code (headings, mono, data figures) + Fira Sans
>   (body / UI), loaded via `next/font/google` as `--font-fira-code` /
>   `--font-fira-sans`.
> - **Key UX guidelines applied:** marker categories pair colour with
>   shape/label + legend (never colour alone); ≥44px touch targets and
>   ≥8px spacing for map/overlay/chat controls; `min-h-dvh` + safe-area
>   insets for the full-screen map and bottom chat drawer; explicit
>   z-index scale (map 0 / overlay 10 / chat 20 / sheet 40 / toast 1000)
>   so React overlays don't fight Leaflet panes; skeletons + typing
>   indicator for >1s waits (stops load, coverage fetch, blocking AI call).

## Error handling

| Case | Handling |
|---|---|
| Any GET/POST non-2xx | `api.ts` throws `ApiError`; the calling panel shows an inline error + retry button. |
| `/api/stops` fails on load | Map still renders (tiles only) with a dismissible error toast; retry re-fetches. |
| `/api/routes/suggest` → `routeFound: false` | Normal result: "No route found between these stops." Not an error. |
| Chat request fails | Error bubble in the thread; user can resend. |
| Leaflet tile load failure | Left to Leaflet's own handling. |
| `next/dynamic` map not yet loaded | Skeleton placeholder in the map area. |

## Testing

**Frontend** — Vitest + React Testing Library:

- `lib/api.ts`: mocked `fetch`; asserts correct URL/params per function,
  typed parsing, and `ApiError` thrown on non-2xx.
- `RoutePlanner`: selection state, "Find route" disabled until both set,
  `routeFound: false` rendering, result card rendering.
- `MessageList` / `ChatPanel`: user + assistant message rendering,
  pending indicator, error bubble.
- Map rendering itself is not unit-tested.

**Backend** — `@WebMvcTest(ChatController.class)` with a mocked
`ChatClient`: `POST /api/chat` with `{ "message": "..." }` returns
`{ "reply": "..." }` and status 200.

## Risks / open items

- **Stop volume**: Nairobi GTFS may have thousands of stops. Clustering
  (`react-leaflet-cluster`) is the mitigation; if still slow, add
  viewport-bounded rendering later.
- **No route geometry**: route planner is textual only until a backend
  endpoint exposes coordinates for a suggested route.
- **shadcn MCP unavailable** this session — CLI is the fallback and is
  sufficient.
- **Chat latency**: blocking agent call with tool use + RAG can take
  several seconds; the typing indicator must make that acceptable.

## Delivery order (for the implementation plan)

1. Backend `ChatController` + `@WebMvcTest`.
2. Scaffold Next.js app, Tailwind, `next.config.ts` rewrites, `.env` example.
3. `lib/types.ts`, `lib/api.ts` + tests.
4. `ui-ux-pro-max` pass → tokens into theme; record choices in this spec.
5. shadcn primitives.
6. `MatatuMap` + `StopMarkers` + `useStops` (map renders with stops).
7. `RoutePlanner` + tests.
8. `CoverageLayer` + `useCoverage`.
9. `ChatPanel` (21st.dev composite) + tests.
10. Page layout: desktop split + mobile `Sheet` drawer + overlay controls.
11. Error/loading states pass; README for `frontend/`.
