package com.matuconnect.graph;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Finds the shortest route between two stops using a custom Dijkstra
 * variant, rather than JGraphT's built-in DijkstraShortestPath.
 * <p>
 * This isn't built-in because plain Dijkstra's search state is just "the
 * current stop" — it has no way to know which route you arrived on, so
 * it cannot penalize switching routes mid-journey. Here, search state is
 * (stop, lastRouteRidden), so a transfer penalty can be applied exactly
 * when the route actually changes. See {@link #TRANSFER_PENALTY_SECONDS}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    /**
     * Penalty, in seconds, added to path cost whenever the search
     * crosses from one route onto a different one. This is a modeling
     * choice, not a measured value — it exists purely to bias path
     * selection toward fewer transfers (matching real commuter
     * preference), and is intentionally excluded from the travel time
     * reported back in {@link RouteResult}, which reflects only actual
     * riding time.
     */
    private static final int TRANSFER_PENALTY_SECONDS = 300;

    private final Graph<String, MatatuEdge> matatuGraph;

    /**
     * Search state: a physical stop plus the route most recently
     * boarded to reach it (null at the origin, before boarding
     * anything). A record fits here — it's an immutable key used only
     * within this algorithm's maps and queue.
     */
    private record SearchState(String stopId, String lastRouteId) {
    }

    private record QueueEntry(SearchState state, int cost) implements Comparable<QueueEntry> {
        @Override
        public int compareTo(QueueEntry other) {
            return Integer.compare(this.cost, other.cost);
        }
    }

    public Optional<RouteResult> findShortestRoute(String originStopId, String destinationStopId) {
        if (!matatuGraph.containsVertex(originStopId)) {
            log.warn("Origin stop {} not found in graph.", originStopId);
            return Optional.empty();
        }
        if (!matatuGraph.containsVertex(destinationStopId)) {
            log.warn("Destination stop {} not found in graph.", destinationStopId);
            return Optional.empty();
        }

        Map<SearchState, Integer> bestCost = new HashMap<>();
        Map<SearchState, SearchState> predecessor = new HashMap<>();
        Map<SearchState, MatatuEdge> predecessorEdge = new HashMap<>();

        SearchState start = new SearchState(originStopId, null);
        bestCost.put(start, 0);

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>();
        queue.add(new QueueEntry(start, 0));

        SearchState goal = null;

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            SearchState state = current.state();
            int cost = current.cost();

            if (cost > bestCost.getOrDefault(state, Integer.MAX_VALUE)) {
                continue; // stale entry — a better path to this state was already found
            }

            if (state.stopId().equals(destinationStopId)) {
                goal = state; // Dijkstra guarantee: first pop of the destination is optimal
                break;
            }

            for (MatatuEdge edge : matatuGraph.outgoingEdgesOf(state.stopId())) {
                String nextStopId = matatuGraph.getEdgeTarget(edge);

                boolean isTransfer = state.lastRouteId() != null
                        && edge.getRouteId() != null
                        && !state.lastRouteId().equals(edge.getRouteId());

                int edgeCost = edge.getTravelTimeSeconds() + (isTransfer ? TRANSFER_PENALTY_SECONDS : 0);
                int newCost = cost + edgeCost;

                SearchState nextState = new SearchState(nextStopId, edge.getRouteId());

                if (newCost < bestCost.getOrDefault(nextState, Integer.MAX_VALUE)) {
                    bestCost.put(nextState, newCost);
                    predecessor.put(nextState, state);
                    predecessorEdge.put(nextState, edge);
                    queue.add(new QueueEntry(nextState, newCost));
                }
            }
        }

        if (goal == null) {
            log.info("No path found between {} and {}.", originStopId, destinationStopId);
            return Optional.empty();
        }

        return Optional.of(reconstructPath(start, goal, predecessor, predecessorEdge));
    }

    private RouteResult reconstructPath(SearchState start, SearchState goal,
                                        Map<SearchState, SearchState> predecessor,
                                        Map<SearchState, MatatuEdge> predecessorEdge) {
        LinkedList<String> stopIds = new LinkedList<>();
        LinkedList<MatatuEdge> edgesUsed = new LinkedList<>();

        SearchState state = goal;
        while (!state.equals(start)) {
            stopIds.addFirst(state.stopId());
            edgesUsed.addFirst(predecessorEdge.get(state));
            state = predecessor.get(state);
        }
        stopIds.addFirst(start.stopId());

        // Actual riding time only — the transfer penalty above was purely
        // for path selection and must not leak into the reported duration.
        int actualRideTimeSeconds = edgesUsed.stream()
                .mapToInt(MatatuEdge::getTravelTimeSeconds)
                .sum();

        LinkedList<String> routeIdsUsed = new LinkedList<>();
        for (MatatuEdge edge : edgesUsed) {
            if (routeIdsUsed.isEmpty() || !routeIdsUsed.getLast().equals(edge.getRouteId())) {
                routeIdsUsed.addLast(edge.getRouteId());
            }
        }
        int transferCount = Math.max(0, routeIdsUsed.size() - 1);

        return new RouteResult(stopIds, List.copyOf(routeIdsUsed), actualRideTimeSeconds, transferCount);
    }
}