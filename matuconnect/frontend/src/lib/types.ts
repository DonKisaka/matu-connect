export interface StopDto {
  stopId: string;
  stopName: string;
  latitude: number;
  longitude: number;
}

export interface RouteAdviceDto {
  routeFound: boolean;
  stopNamesInOrder: string[];
  stopsInOrder: StopDto[];
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

/** One sampled grid point farther than the threshold from any stop. */
export interface GapPointDto {
  latitude: number;
  longitude: number;
  nearestStopMetres: number;
}

export interface WalkingDistanceGapDto {
  gaps: GapPointDto[];
  gridPointsSampled: number;
  thresholdMetres: number;
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

/** A route created or edited through the admin dashboard. */
export interface AdminRouteDto {
  routeId: string;
  routeShortName: string;
  routeLongName: string;
  stopsInOrder: StopDto[];
}

/**
 * Body of a create/update request. `minutesFromPrevious` is ignored on the
 * first stop — there's no previous stop to measure the travel time from.
 */
export interface RouteEditRequest {
  routeShortName: string;
  routeLongName: string;
  stops: { stopId: string; minutesFromPrevious: number | null }[];
}
