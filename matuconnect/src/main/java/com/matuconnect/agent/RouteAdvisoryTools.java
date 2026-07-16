package com.matuconnect.agent;


import com.matuconnect.graph.CoverageAnalysisService;
import com.matuconnect.graph.CoverageGapResult;
import com.matuconnect.graph.RouteResult;
import com.matuconnect.graph.RoutingService;
import com.matuconnect.model.Route;
import com.matuconnect.model.Stop;
import com.matuconnect.repository.RouteRepository;
import com.matuconnect.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Tools exposed to the Spring AI chat agent. Each {@code @Tool} method is
 * a bridge between the LLM's natural-language reasoning and the
 * deterministic graph services built earlier — the model never touches
 * the graph directly, it only sees these narrow, purpose-built methods.
 * <p>
 * Kept as one class for now since the tool set is small (3 methods) and
 * all of it is "route advisory" — if the agent gains many more tools
 * later, splitting by concern (e.g. a separate CoverageTools) would be
 * worth revisiting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteAdvisoryTools {

    /**
     * Cap on how many isolated-cluster / poorly-served stop names are
     * surfaced to the model per call — the underlying analysis can
     * involve hundreds of stops, which would be both unreadable in a
     * chat response and wasteful of context tokens.
     */
    private static final int MAX_EXAMPLE_STOPS = 5;

    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final RoutingService routingService;
    private final CoverageAnalysisService coverageAnalysisService;

    @Tool(description = "Search for matatu stops by name or partial name (case-insensitive). " +
            "Always use this first to resolve a place name the user mentions into a stop_id " +
            "before calling suggestRoute — never guess a stop_id.")
    public List<StopSummary> findStopsByName(
            @ToolParam(description = "Partial or full stop name, e.g. 'Kencom' or 'Odeon'") String query) {

        return stopRepository.findByStopNameContainingIgnoreCase(query).stream()
                .map(stop -> new StopSummary(stop.getStopId(), stop.getStopName(), stop.getStopLat(), stop.getStopLon()))
                .limit(10)
                .toList();
    }

    @Tool(description = "Suggest the best matatu route between two stops, identified by stop_id " +
            "(obtained from findStopsByName). Returns the ordered stops, the routes to board, " +
            "estimated ride time in minutes, and how many transfers are required.")
    public RouteAdvisoryResponse suggestRoute(
            @ToolParam(description = "stop_id of the origin, from findStopsByName") String originStopId,
            @ToolParam(description = "stop_id of the destination, from findStopsByName") String destinationStopId) {

        Optional<RouteResult> result = routingService.findShortestRoute(originStopId, destinationStopId);

        if (result.isEmpty()) {
            return new RouteAdvisoryResponse(false, List.of(), List.of(), 0, 0);
        }

        RouteResult route = result.get();

        List<String> stopNames = route.stopIds().stream()
                .map(this::resolveStopName)
                .toList();

        List<String> routeNames = route.routeIdsUsed().stream()
                .map(this::resolveRouteName)
                .toList();

        int estimatedMinutes = (int) Math.ceil(route.totalTravelTimeSeconds() / 60.0);

        return new RouteAdvisoryResponse(true, stopNames, routeNames, estimatedMinutes, route.transferCount());
    }

    @Tool(description = "Get a summary of matatu network coverage gaps — stops or clusters of stops " +
            "that are isolated from the main network, and stops with unusually few route options. " +
            "Use this for general questions like 'which areas are poorly served?' rather than for " +
            "point-to-point route requests.")
    public CoverageGapSummary getCoverageGapSummary() {
        CoverageGapResult result = coverageAnalysisService.analyzeCoverage();

        List<String> exampleIsolatedNames = result.isolatedClusters().stream()
                .flatMap(cluster -> cluster.stopIds().stream())
                .limit(MAX_EXAMPLE_STOPS)
                .map(this::resolveStopName)
                .toList();

        List<String> worstServedNames = result.poorlyServedStops().stream()
                .limit(MAX_EXAMPLE_STOPS)
                .map(stop -> resolveStopName(stop.stopId()))
                .toList();

        return new CoverageGapSummary(
                result.totalStops(),
                result.mainComponentSize(),
                result.isolatedClusters().size(),
                exampleIsolatedNames,
                worstServedNames
        );
    }

    private String resolveStopName(String stopId) {
        return stopRepository.findById(stopId)
                .map(Stop::getStopName)
                .orElse(stopId); // fall back to the raw ID rather than throwing — a missing
        // lookup shouldn't break an otherwise-successful route response
    }

    private String resolveRouteName(String routeId) {
        if (routeId == null) {
            return "unknown route";
        }
        return routeRepository.findById(routeId)
                .map(Route::getRouteShortName)
                .orElse(routeId);
    }
}