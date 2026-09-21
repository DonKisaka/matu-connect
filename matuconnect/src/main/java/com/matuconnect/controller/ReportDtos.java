package com.matuconnect.controller;


import com.matuconnect.model.JourneySearch;
import com.matuconnect.report.PopularRoute;

import java.time.Instant;

/**
 * REST-facing shapes for the reporting endpoints. Kept separate from
 * {@link PopularRoute}, which is a query projection owned by the report
 * package — the wire format should be free to change without forcing a
 * change to the JPQL that produces it.
 */

/**
 * A row of the most-searched-routes report. {@code successRatePercent} is
 * pre-computed rather than left to the client, so every consumer reports the
 * same number.
 */
record PopularRouteDto(
        String originStopId,
        String originStopName,
        String destinationStopId,
        String destinationStopName,
        long searchCount,
        long foundCount,
        int successRatePercent
) {
    static PopularRouteDto from(PopularRoute route) {
        return new PopularRouteDto(
                route.originStopId(),
                route.originStopName(),
                route.destinationStopId(),
                route.destinationStopName(),
                route.searchCount(),
                route.foundCount(),
                route.successRatePercent());
    }
}

/**
 * Headline figures for the administrator dashboard: who is using the system,
 * how much they search, and how the network itself is shaped.
 */
record UsageStatsDto(
        long totalUsers,
        long adminUsers,
        long totalSearches,
        long successfulSearches,
        long failedSearches,
        int totalStops,
        int mainNetworkSize,
        int isolatedClusterCount
) {
}

/**
 * One entry in a user's journey history. Stop names are resolved here rather
 * than left as ids, because this is read by the person who made the search.
 */
record JourneySearchDto(
        Long id,
        String originStopId,
        String originStopName,
        String destinationStopId,
        String destinationStopName,
        boolean routeFound,
        Integer transferCount,
        Integer estimatedMinutes,
        Instant searchedAt
) {
    static JourneySearchDto from(JourneySearch search) {
        return new JourneySearchDto(
                search.getId(),
                search.getOriginStop().getStopId(),
                search.getOriginStop().getStopName(),
                search.getDestinationStop().getStopId(),
                search.getDestinationStop().getStopName(),
                search.isRouteFound(),
                search.getTransferCount(),
                search.getEstimatedMinutes(),
                search.getSearchedAt());
    }
}
