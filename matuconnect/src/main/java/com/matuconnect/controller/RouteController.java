package com.matuconnect.controller;


import com.matuconnect.graph.RouteResult;
import com.matuconnect.graph.RoutingService;
import com.matuconnect.model.Route;
import com.matuconnect.model.Stop;
import com.matuconnect.repository.RouteRepository;
import com.matuconnect.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Direct route-suggestion endpoint, bypassing the chat agent entirely.
 * Intended for the map UI: "click origin, click destination, draw the
 * path" shouldn't require a natural-language round-trip through an LLM
 * just to get back structured stop/route data it already knows how to
 * request directly.
 * <p>
 * Deliberately returns HTTP 200 with {@code routeFound: false} rather
 * than a 404 when no path exists — "no route between these two stops"
 * is a normal, expected outcome for this graph (see
 * CoverageAnalysisService's isolated-cluster analysis), not an error
 * condition or a missing resource.
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RoutingService routingService;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;

    @GetMapping("/suggest")
    public RouteAdviceDto suggestRoute(@RequestParam String originStopId,
                                       @RequestParam String destinationStopId) {

        Optional<RouteResult> result = routingService.findShortestRoute(originStopId, destinationStopId);

        if (result.isEmpty()) {
            return new RouteAdviceDto(false, List.of(), List.of(), 0, 0);
        }

        RouteResult route = result.get();

        List<String> stopNames = route.stopIds().stream()
                .map(this::resolveStopName)
                .toList();

        List<String> routeNames = route.routeIdsUsed().stream()
                .map(this::resolveRouteName)
                .toList();

        int estimatedMinutes = (int) Math.ceil(route.totalTravelTimeSeconds() / 60.0);

        return new RouteAdviceDto(true, stopNames, routeNames, estimatedMinutes, route.transferCount());
    }

    private String resolveStopName(String stopId) {
        return stopRepository.findById(stopId).map(Stop::getStopName).orElse(stopId);
    }

    private String resolveRouteName(String routeId) {
        if (routeId == null) {
            return "unknown route";
        }
        return routeRepository.findById(routeId).map(Route::getRouteShortName).orElse(routeId);
    }
}