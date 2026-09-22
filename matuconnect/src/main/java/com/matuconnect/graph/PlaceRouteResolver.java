package com.matuconnect.graph;


import com.matuconnect.model.Stop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a journey between two place NAMES rather than two exact
 * stop_ids, by evaluating every served candidate pair and keeping the
 * best actual route.
 * <p>
 * Nairobi's feed has many stops sharing one name — eleven match "Ngara",
 * fourteen match "Limuru" — and only some of the resulting pairs are
 * actually connected. A caller (a combobox, a chat agent) that resolves
 * a name to a single specific stop_id can easily land on one that
 * happens not to connect, even though a same-named sibling does, which
 * produces a wrong "no route exists" answer. This is shared by both the
 * chat agent ({@code RouteAdvisoryTools}) and the direct map route
 * planner ({@code RouteController}) so the fix lives in one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceRouteResolver {

    /**
     * Upper bound on same-named stops considered per side. Routing itself
     * is cheap on a graph this size, but the cap keeps a single resolution
     * predictable rather than an unbounded cross-product.
     */
    private static final int MAX_CANDIDATES_PER_PLACE = 8;

    private final StopSearchService stopSearchService;
    private final RoutingService routingService;

    public record Resolution(
            RouteResult route,
            Stop origin,
            Stop destination,
            int candidatePairsEvaluated
    ) {
    }

    public Optional<Resolution> resolveBestRoute(String originName, String destinationName) {
        List<Stop> origins = stopSearchService.searchServedStops(originName).stream()
                .limit(MAX_CANDIDATES_PER_PLACE).toList();
        List<Stop> destinations = stopSearchService.searchServedStops(destinationName).stream()
                .limit(MAX_CANDIDATES_PER_PLACE).toList();

        if (origins.isEmpty() || destinations.isEmpty()) {
            log.info("No served stops matched origin '{}' ({}) or destination '{}' ({}).",
                    originName, origins.size(), destinationName, destinations.size());
            return Optional.empty();
        }

        RouteResult best = null;
        Stop bestOrigin = null;
        Stop bestDestination = null;
        int evaluated = 0;

        for (Stop origin : origins) {
            for (Stop destination : destinations) {
                if (origin.getStopId().equals(destination.getStopId())) {
                    continue;
                }
                evaluated++;
                Optional<RouteResult> candidate =
                        routingService.findShortestRoute(origin.getStopId(), destination.getStopId());
                if (candidate.isEmpty()) {
                    continue;
                }
                if (best == null || isPreferable(candidate.get(), best)) {
                    best = candidate.get();
                    bestOrigin = origin;
                    bestDestination = destination;
                }
            }
        }

        if (best == null) {
            log.info("No route between '{}' and '{}' across {} candidate pairs.",
                    originName, destinationName, evaluated);
            return Optional.empty();
        }

        log.info("Best journey '{}' -> '{}': via {}/{} of {} pairs.",
                originName, destinationName, bestOrigin.getStopId(), bestDestination.getStopId(), evaluated);

        return Optional.of(new Resolution(best, bestOrigin, bestDestination, evaluated));
    }

    /**
     * Fewer transfers wins, because that is what commuters actually optimise
     * for; ride time only breaks a tie. Ordering by time first would favour
     * journeys that shave a few minutes by changing matatu three more times.
     */
    private static boolean isPreferable(RouteResult candidate, RouteResult incumbent) {
        if (candidate.transferCount() != incumbent.transferCount()) {
            return candidate.transferCount() < incumbent.transferCount();
        }
        return candidate.totalTravelTimeSeconds() < incumbent.totalTravelTimeSeconds();
    }
}
