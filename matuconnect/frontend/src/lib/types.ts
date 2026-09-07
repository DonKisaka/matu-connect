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
