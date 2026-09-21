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

export type Role = "COMMUTER" | "ADMIN";

export interface AuthUser {
  id: number;
  username: string;
  role: Role;
}

/** Headline figures for the administrator dashboard. */
export interface UsageStats {
  totalUsers: number;
  adminUsers: number;
  totalSearches: number;
  successfulSearches: number;
  failedSearches: number;
  totalStops: number;
  mainNetworkSize: number;
  isolatedClusterCount: number;
}

/**
 * One row of the most-searched-routes report. `successRatePercent` is
 * computed server-side so every consumer reports the same figure.
 */
export interface PopularRoute {
  originStopId: string;
  originStopName: string;
  destinationStopId: string;
  destinationStopName: string;
  searchCount: number;
  foundCount: number;
  successRatePercent: number;
}

/** One entry in the signed-in user's journey history. */
export interface JourneySearchEntry {
  id: number;
  originStopId: string;
  originStopName: string;
  destinationStopId: string;
  destinationStopName: string;
  routeFound: boolean;
  transferCount: number | null;
  estimatedMinutes: number | null;
  searchedAt: string;
}
