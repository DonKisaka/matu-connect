package com.matuconnect.graph;


import com.matuconnect.model.Stop;
import com.matuconnect.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Name search over stops that are actually reachable in the network.
 * <p>
 * stops.txt contains a small number of stops that no trip in stop_times.txt
 * ever serves (11 of 4284 in the Digital Matatus feed). They are real
 * entries in the feed but have no edges in the graph, so no journey can
 * ever start or end at one. Returning them from a search is actively
 * harmful: "Kencom/Ambassadeur" resolves to four different stop_ids, and
 * the unserved one sorts first, so both a commuter using the search box
 * and the AI agent calling {@code findStopsByName} would pick a stop that
 * can never produce a route.
 * <p>
 * Filtering happens against the graph rather than by re-querying
 * stop_times, because the graph is already the authority on what is
 * connected — a stop with zero degree there is unreachable by definition,
 * whatever the raw feed says.
 * <p>
 * Note this deliberately does not filter {@code GET /api/stops}: the map
 * legitimately plots every documented stop, including ones no trip
 * currently serves. Only search, which feeds route planning, is filtered.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StopSearchService {

    private final StopRepository stopRepository;
    private final MatatuGraphHolder matatuGraphHolder;

    /**
     * Stops whose name contains {@code query} (case-insensitive) and which
     * at least one trip actually serves.
     */
    public List<Stop> searchServedStops(String query) {
        List<Stop> matches = stopRepository.findByStopNameContainingIgnoreCase(query);

        List<Stop> served = matches.stream()
                .filter(stop -> isServed(stop.getStopId()))
                .toList();

        if (served.size() < matches.size()) {
            log.debug("Stop search '{}': filtered out {} unserved stop(s) of {} name matches.",
                    query, matches.size() - served.size(), matches.size());
        }
        return served;
    }

    /**
     * Whether any trip serves this stop — i.e. whether it has any edge in
     * the network graph.
     */
    public boolean isServed(String stopId) {
        Graph<String, MatatuEdge> matatuGraph = matatuGraphHolder.get();
        return matatuGraph.containsVertex(stopId) && matatuGraph.degreeOf(stopId) > 0;
    }
}
