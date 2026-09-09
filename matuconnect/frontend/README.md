# MatuConnect Frontend

Next.js 16 + Leaflet map and chat UI for the MatuConnect Nairobi matatu
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
- `src/components/{MapOverlays,ChatDock}.tsx` — the overlay stack and the desktop/mobile chat dock
- `src/lib/api.ts` — typed API client (all endpoints)
- `src/hooks/*` — `useStops`, `useCoverage`

## Known limitations

- The route planner is textual — `/api/routes/suggest` returns stop
  names only, no coordinates, so no route polyline is drawn.
- Chat has no server-side memory; each message is independent.
- Chat state is per-instance: the desktop panel and the mobile drawer
  keep separate histories, so resizing across the `lg` breakpoint
  mid-conversation starts fresh.
