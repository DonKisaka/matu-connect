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
 * A journey found between two places named in plain language, rather than
 * between two specific stop_ids.
 * <p>
 * Nairobi's feed contains many distinct stops sharing one name — "Limuru
 * Terminus" is two different stop_ids, and only one of them is reachable
 * from Ngara. Naming which stops were actually used therefore matters:
 * without it the model cannot tell the user that the journey it found
 * starts from a particular one of several same-named stages.
 * {@code candidatePairsEvaluated} is carried so the model can say how
 * thoroughly it looked before reporting that nothing connects.
 */
record PlaceRouteResponse(
        boolean routeFound,
        String originStopName,
        String originStopId,
        String destinationStopName,
        String destinationStopId,
        List<String> stopNamesInOrder,
        List<String> routeNamesUsed,
        int estimatedRideMinutes,
        int transferCount,
        int candidatePairsEvaluated
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
