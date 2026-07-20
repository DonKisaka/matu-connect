package com.matuconnect.controller;


import java.util.List;

/**
 * REST-facing shape for a stop. Deliberately separate from the agent
 * package's {@code StopSummary} record — that one is package-private and
 * scoped to what the LLM needs; this is the public contract the frontend
 * depends on, and the two shouldn't be coupled just because they
 * currently look similar.
 */
public record StopDto(String stopId, String stopName, double latitude, double longitude) {
}

/**
 * REST-facing shape for a route suggestion, returned directly to the
 * frontend map — no LLM involved in this path.
 */
public record RouteAdviceDto(
        boolean routeFound,
        List<String> stopNamesInOrder,
        List<String> routeNamesUsed,
        int estimatedRideMinutes,
        int transferCount
) {
}

/**
 * REST-facing shape for a coverage gap summary.
 */
public record CoverageDto(
        int totalStops,
        int mainNetworkSize,
        int isolatedClusterCount,
        List<StopDto> exampleIsolatedStops,
        List<StopDto> worstServedStops
) {
}

/**
 * Request/response shapes for the chat endpoint.
 */
record ChatRequest(String message) {
}

record ChatResponse(String reply) {
}