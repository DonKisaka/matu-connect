package com.matuconnect.controller;


import java.util.List;

/**
 * Body of a route create/update request from the admin dashboard.
 * <p>
 * {@code minutesFromPrevious} is the travel time from the previous stop in
 * the list, in minutes — ignored on the first entry, since there's no
 * previous stop to measure from. This is deliberately simpler than a full
 * GTFS timetable: {@code MatatuGraphBuilder} only ever uses the time
 * <em>delta</em> between consecutive stop_times to weight an edge, never the
 * absolute clock time, so an admin never has to invent a fake departure
 * schedule just to add a route.
 */
public record RouteEditRequest(
        String routeShortName,
        String routeLongName,
        List<StopEntry> stops
) {
    public record StopEntry(String stopId, Integer minutesFromPrevious) {
    }
}
