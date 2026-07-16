package com.matuconnect.graph;


import lombok.Getter;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * A directed edge between two consecutive stops on a single trip.
 * <p>
 * Extends {@link DefaultWeightedEdge} so JGraphT's algorithms (Dijkstra,
 * connectivity inspectors, etc.) work on it unmodified, but carries the
 * route/trip it belongs to — needed by {@link RoutingService} to detect
 * when a shortest path crosses from one route onto another and apply a
 * transfer penalty.
 */
@Getter
public class MatatuEdge extends DefaultWeightedEdge {

    private final String routeId;
    private final String tripId;


    private final int travelTimeSeconds;

    public MatatuEdge(String routeId, String tripId, int travelTimeSeconds) {
        this.routeId = routeId;
        this.tripId = tripId;
        this.travelTimeSeconds = travelTimeSeconds;
    }

    @Override
    public String toString() {
        return "MatatuEdge{route=" + routeId + ", trip=" + tripId
                + ", travelTimeSeconds=" + travelTimeSeconds + "}";
    }
}