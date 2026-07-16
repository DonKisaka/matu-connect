package com.matuconnect.graph;


import com.matuconnect.model.Stop;
import com.matuconnect.model.StopTime;
import com.matuconnect.model.Trip;
import com.matuconnect.repository.StopRepository;
import com.matuconnect.repository.StopTimeRepository;
import com.matuconnect.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the matatu network as a directed, edge-weighted multigraph:
 * <ul>
 *   <li>Vertices are physical stops, identified by {@code stop_id}.</li>
 *   <li>Edges connect two stops that are consecutive within a single
 *       trip's stop_sequence, weighted by travel time in seconds.</li>
 *   <li>A multigraph (not a simple graph) is used deliberately — two
 *       different routes can directly connect the same pair of stops,
 *       and collapsing those into one edge would silently discard the
 *       fact that a commuter has a choice of route between them.</li>
 * </ul>
 * The graph is directed because direction_id 0 and 1 trips are not
 * guaranteed to follow the same physical stop sequence in reverse
 * (matatus can differ by one-way streets), so an undirected graph would
 * risk suggesting paths no matatu actually runs.
 * <p>
 * This class only builds the graph; it does not decide when to rebuild
 * it. See {@link MatatuGraphConfig} for how it's exposed as a singleton
 * Spring bean.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatatuGraphBuilder {

    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;

    public Graph<String, MatatuEdge> buildGraph() {
        Graph<String, MatatuEdge> graph = new DirectedWeightedMultigraph<>(MatatuEdge.class);

        List<Stop> stops = stopRepository.findAll();
        stops.forEach(stop -> graph.addVertex(stop.getStopId()));
        log.info("Graph builder: added {} stop vertices.", stops.size());

        List<Trip> trips = tripRepository.findAll();
        int edgeCount = 0;
        int skippedTrips = 0;

        for (Trip trip : trips) {
            List<StopTime> orderedStopTimes =
                    stopTimeRepository.findByTrip_TripIdOrderById_StopSequenceAsc(trip.getTripId());

            if (orderedStopTimes.size() < 2) {
                // A trip with 0 or 1 stop_times rows can't form an edge — log and move on
                // rather than let a malformed trip silently do nothing without a trace.
                skippedTrips++;
                continue;
            }

            String routeId = trip.getRoute() != null ? trip.getRoute().getRouteId() : null;

            for (int i = 0; i < orderedStopTimes.size() - 1; i++) {
                StopTime from = orderedStopTimes.get(i);
                StopTime to = orderedStopTimes.get(i + 1);

                String fromStopId = from.getStop().getStopId();
                String toStopId = to.getStop().getStopId();

                if (!graph.containsVertex(fromStopId) || !graph.containsVertex(toStopId)) {
                    // Defensive: shouldn't happen since every Stop was added above,
                    // but guards against a dangling stop_id if ingestion ever skips one.
                    continue;
                }

                int travelTimeSeconds = to.getArrivalTime() - from.getDepartureTime();
                if (travelTimeSeconds < 0) {
                    // Guards against malformed GTFS rows (e.g. out-of-order times).
                    // A negative-weight edge would break Dijkstra's correctness.
                    log.warn("Skipping negative-duration edge on trip {} between {} and {}",
                            trip.getTripId(), fromStopId, toStopId);
                    continue;
                }

                MatatuEdge edge = new MatatuEdge(routeId, trip.getTripId(), travelTimeSeconds);
                graph.addEdge(fromStopId, toStopId, edge);
                graph.setEdgeWeight(edge, travelTimeSeconds);
                edgeCount++;
            }
        }

        log.info("Graph builder: added {} edges from {} trips ({} trips skipped — fewer than 2 stop_times).",
                edgeCount, trips.size(), skippedTrips);

        return graph;
    }
}
