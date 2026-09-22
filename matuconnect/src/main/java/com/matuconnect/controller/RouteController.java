package com.matuconnect.controller;


import com.matuconnect.graph.PlaceRouteResolver;
import com.matuconnect.graph.RouteResult;
import com.matuconnect.graph.RoutingService;
import com.matuconnect.report.JourneySearchLogService;
import com.matuconnect.model.Route;
import com.matuconnect.model.Stop;
import com.matuconnect.repository.RouteRepository;
import com.matuconnect.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final PlaceRouteResolver placeRouteResolver;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final JourneySearchLogService journeySearchLogService;

    @GetMapping("/suggest")
    public RouteAdviceDto suggestRoute(@RequestParam String originStopId,
                                       @RequestParam String destinationStopId,
                                       Authentication authentication) {

        Optional<RouteResult> result = routingService.findShortestRoute(originStopId, destinationStopId);

        // Recorded for the usage reports and journey history. Deliberately also
        // recorded when nothing was found: a pair people keep asking for and the
        // network cannot serve is demand-weighted evidence of a coverage gap.
        // `authentication` is null for a signed-out caller, which is allowed —
        // the search is then recorded unattributed. The call cannot throw.
        String username = authentication != null ? authentication.getName() : null;
        journeySearchLogService.record(originStopId, destinationStopId, result, username);

        if (result.isEmpty()) {
            return new RouteAdviceDto(false, List.of(), List.of(), List.of(), 0, 0);
        }

        RouteResult route = result.get();

        List<String> stopNames = route.stopIds().stream()
                .map(this::resolveStopName)
                .toList();

        List<StopDto> stopsInOrder = route.stopIds().stream()
                .map(this::resolveStop)
                .toList();

        List<String> routeNames = route.routeIdsUsed().stream()
                .map(this::resolveRouteName)
                .toList();

        int estimatedMinutes = (int) Math.ceil(route.totalTravelTimeSeconds() / 60.0);

        return new RouteAdviceDto(
                true, stopNames, stopsInOrder, routeNames, estimatedMinutes, route.transferCount());
    }

    /**
     * Same as {@link #suggestRoute}, but takes place NAMES rather than exact
     * stop_ids.
     * <p>
     * The map's stop search box lets a commuter pick a specific stop_id from
     * several that share one name (Nairobi has eleven stops named "Ngara"),
     * and only some of those same-named stops are actually connected to each
     * other — so a click that happens to land on an unconnected one used to
     * report "no route found" even though the place itself is served. This
     * evaluates every served candidate pair for the two names and returns
     * the best actual journey, exactly like the chat agent's
     * {@code suggestRouteBetweenPlaces} tool already did — this endpoint
     * gives the direct map path the same guarantee.
     */
    @GetMapping("/suggest-by-name")
    public RouteAdviceDto suggestRouteByName(@RequestParam String originName,
                                             @RequestParam String destinationName,
                                             Authentication authentication) {

        Optional<PlaceRouteResolver.Resolution> resolution =
                placeRouteResolver.resolveBestRoute(originName, destinationName);

        if (resolution.isEmpty()) {
            return new RouteAdviceDto(false, List.of(), List.of(), List.of(), 0, 0);
        }

        PlaceRouteResolver.Resolution best = resolution.get();
        RouteResult route = best.route();

        String username = authentication != null ? authentication.getName() : null;
        journeySearchLogService.record(
                best.origin().getStopId(), best.destination().getStopId(), Optional.of(route), username);

        List<String> stopNames = route.stopIds().stream().map(this::resolveStopName).toList();
        List<StopDto> stopsInOrder = route.stopIds().stream().map(this::resolveStop).toList();
        List<String> routeNames = route.routeIdsUsed().stream().map(this::resolveRouteName).toList();
        int estimatedMinutes = (int) Math.ceil(route.totalTravelTimeSeconds() / 60.0);

        return new RouteAdviceDto(
                true, stopNames, stopsInOrder, routeNames, estimatedMinutes, route.transferCount());
    }

    private String resolveStopName(String stopId) {
        return stopRepository.findById(stopId).map(Stop::getStopName).orElse(stopId);
    }

    private StopDto resolveStop(String stopId) {
        return stopRepository.findById(stopId)
                .map(stop -> new StopDto(stop.getStopId(), stop.getStopName(), stop.getStopLat(), stop.getStopLon()))
                .orElse(new StopDto(stopId, stopId, 0.0, 0.0));
    }

    private String resolveRouteName(String routeId) {
        if (routeId == null) {
            return "unknown route";
        }
        return routeRepository.findById(routeId).map(Route::getRouteShortName).orElse(routeId);
    }
}