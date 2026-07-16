package com.matuconnect.graph;


import java.util.List;

/**
 * The result of a route query: the ordered stops to travel through,
 * total travel time, and how many route changes are required.
 * A record fits here deliberately — this is an immutable value object
 * returned from a computation, not a persisted/mutable JPA entity.
 */
public record RouteResult(
        List<String> stopIds,
        List<String> routeIdsUsed,
        int totalTravelTimeSeconds,
        int transferCount
) {
}
