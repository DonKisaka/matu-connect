package com.matuconnect.agent;


import java.util.List;

/**
 * A stop returned from a name search — includes the stop_id the model
 * needs for follow-up tool calls, plus the name/coordinates so it can
 * confirm the right stop with the user if the search matched several.
 */
record StopSummary(String stopId, String stopName, double latitude, double longitude) {
}

/**
 * Human-readable result of a route suggestion. Deliberately resolves
 * stop_ids and route_ids into names before returning — the model should
 * reason about "Kencom" and "Route 46", not raw GTFS identifiers it has
 * no grounding for and could misreport.
 */
record RouteAdvisoryResponse(
        boolean routeFound,
        List<String> stopNamesInOrder,
        List<String> routeNamesUsed,
        int estimatedRideMinutes,
        int transferCount
) {
}

/**
 * A capped, human-readable summary of network coverage gaps — never the
 * full raw lists from CoverageGapResult, which could be hundreds of
 * stops and would blow past what's useful in a chat response.
 */
record CoverageGapSummary(
        int totalStops,
        int mainNetworkSize,
        int isolatedClusterCount,
        List<String> exampleIsolatedStopNames,
        List<String> worstServedStopNames
) {
}
