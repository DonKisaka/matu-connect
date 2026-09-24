package com.matuconnect.admin;


import com.matuconnect.controller.AdminRouteDto;
import com.matuconnect.controller.RouteEditRequest;
import com.matuconnect.controller.StopDto;
import com.matuconnect.graph.MatatuGraphHolder;
import com.matuconnect.model.Route;
import com.matuconnect.model.Stop;
import com.matuconnect.model.StopTime;
import com.matuconnect.model.StopTimeId;
import com.matuconnect.model.Trip;
import com.matuconnect.repository.RouteRepository;
import com.matuconnect.repository.StopRepository;
import com.matuconnect.repository.StopTimeRepository;
import com.matuconnect.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lets an administrator add a route the static GTFS feed doesn't know about
 * yet — a newly gazetted service, or one using new infrastructure such as
 * the Nairobi Expressway — without re-running the whole GTFS ingestion.
 * <p>
 * Scoped deliberately narrow: an admin picks an ordered sequence of stops
 * that <em>already exist</em> in the ingested feed, plus the travel time
 * between each consecutive pair. Creating brand-new stops (with real-world
 * coordinates to validate, geocode, etc.) is out of scope — seeded stop data
 * is enough to model any route that connects places the network already
 * knows about, which covers the motivating case (a new route between
 * existing stages) without the much larger surface area of a full stop
 * editor.
 * <p>
 * Every route this service creates gets a {@value #ADMIN_ROUTE_PREFIX}
 * prefixed route_id. Update and delete only ever touch routes with that
 * prefix, so this can never corrupt a real route ingested from the GTFS
 * feed itself.
 * <p>
 * None of this is visible to the routing/coverage algorithms until
 * {@link MatatuGraphHolder#rebuild()} runs — see {@link #rebuildNetwork()}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAdminService {

    public static final String ADMIN_ROUTE_PREFIX = "ADMIN-";

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;
    private final StopRepository stopRepository;
    private final MatatuGraphHolder matatuGraphHolder;

    public List<AdminRouteDto> listAdminRoutes() {
        return routeRepository.findByRouteIdStartingWith(ADMIN_ROUTE_PREFIX).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AdminRouteDto createRoute(RouteEditRequest request) {
        List<Stop> stops = resolveAndValidateStops(request);

        Route route = new Route();
        route.setRouteId(ADMIN_ROUTE_PREFIX + UUID.randomUUID().toString().substring(0, 8));
        route.setRouteShortName(request.routeShortName());
        route.setRouteLongName(request.routeLongName());
        route.setRouteType(3); // Bus — matatus are modeled as buses throughout this project.
        routeRepository.save(route);

        writeTripAndStopTimes(route, stops, request.stops());

        log.info("Admin created route {} ({}) with {} stops.",
                route.getRouteId(), route.getRouteShortName(), stops.size());
        return toDto(route, stops);
    }

    @Transactional
    public AdminRouteDto updateRoute(String routeId, RouteEditRequest request) {
        Route route = requireAdminRoute(routeId);
        List<Stop> stops = resolveAndValidateStops(request);

        route.setRouteShortName(request.routeShortName());
        route.setRouteLongName(request.routeLongName());
        routeRepository.save(route);

        // Simplest correct approach: replace the route's one synthetic trip
        // outright rather than diffing the old and new stop sequences.
        deleteTripsAndStopTimes(route);
        writeTripAndStopTimes(route, stops, request.stops());

        log.info("Admin updated route {} ({}) — now {} stops.",
                route.getRouteId(), route.getRouteShortName(), stops.size());
        return toDto(route, stops);
    }

    @Transactional
    public void deleteRoute(String routeId) {
        Route route = requireAdminRoute(routeId);
        deleteTripsAndStopTimes(route);
        routeRepository.delete(route);
        log.info("Admin deleted route {}.", routeId);
    }

    /**
     * Rebuilds the in-memory routing graph from the database so that
     * create/update/delete actually take effect on the next route search —
     * see {@link MatatuGraphHolder}'s own Javadoc for why this is a
     * deliberate, explicit action rather than automatic on every save.
     */
    public void rebuildNetwork() {
        matatuGraphHolder.rebuild();
    }

    private Route requireAdminRoute(String routeId) {
        if (routeId == null || !routeId.startsWith(ADMIN_ROUTE_PREFIX)) {
            throw new RouteAdminException(
                    "Only routes created through this editor can be edited or deleted.");
        }
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteAdminException("No such route: " + routeId));
    }

    private List<Stop> resolveAndValidateStops(RouteEditRequest request) {
        if (request.stops() == null || request.stops().size() < 2) {
            throw new RouteAdminException("A route needs at least 2 stops.");
        }
        if (request.routeShortName() == null || request.routeShortName().isBlank()) {
            throw new RouteAdminException("A route needs a short name — the number a commuter would look for.");
        }

        List<Stop> stops = new ArrayList<>(request.stops().size());
        for (int i = 0; i < request.stops().size(); i++) {
            RouteEditRequest.StopEntry entry = request.stops().get(i);
            Stop stop = stopRepository.findById(entry.stopId())
                    .orElseThrow(() -> new RouteAdminException("No such stop: " + entry.stopId()));
            stops.add(stop);

            if (i > 0) {
                Integer minutes = entry.minutesFromPrevious();
                if (minutes == null || minutes <= 0) {
                    throw new RouteAdminException(
                            "Travel time from the previous stop must be a positive number of minutes "
                                    + "(stop " + (i + 1) + ": " + entry.stopId() + ").");
                }
            }
        }
        return stops;
    }

    private void writeTripAndStopTimes(Route route, List<Stop> stops, List<RouteEditRequest.StopEntry> entries) {
        Trip trip = new Trip();
        trip.setTripId(route.getRouteId() + "-T1");
        trip.setRoute(route);
        trip.setServiceId("ADMIN");
        trip.setDirectionId(0);
        tripRepository.save(trip);

        // Absolute times are arbitrary — only the delta between consecutive
        // stop_times feeds the graph's edge weights (see MatatuGraphBuilder),
        // so starting the clock at 0 costs nothing.
        int cumulativeSeconds = 0;
        List<StopTime> stopTimes = new ArrayList<>(stops.size());
        for (int i = 0; i < stops.size(); i++) {
            if (i > 0) {
                cumulativeSeconds += entries.get(i).minutesFromPrevious() * 60;
            }
            StopTime stopTime = new StopTime();
            stopTime.setId(new StopTimeId(trip.getTripId(), i + 1));
            stopTime.setTrip(trip);
            stopTime.setStop(stops.get(i));
            stopTime.setArrivalTime(cumulativeSeconds);
            stopTime.setDepartureTime(cumulativeSeconds);
            stopTimes.add(stopTime);
        }
        stopTimeRepository.saveAll(stopTimes);
    }

    private void deleteTripsAndStopTimes(Route route) {
        List<Trip> trips = tripRepository.findByRoute_RouteId(route.getRouteId());
        for (Trip trip : trips) {
            stopTimeRepository.deleteByTrip_TripId(trip.getTripId());
        }
        tripRepository.deleteAll(trips);
    }

    private AdminRouteDto toDto(Route route) {
        List<Trip> trips = tripRepository.findByRoute_RouteId(route.getRouteId());
        List<Stop> stops = trips.stream().findFirst()
                .map(trip -> stopTimeRepository.findByTrip_TripIdOrderById_StopSequenceAsc(trip.getTripId()))
                .orElse(List.of())
                .stream().map(StopTime::getStop).toList();
        return toDto(route, stops);
    }

    private AdminRouteDto toDto(Route route, List<Stop> stops) {
        List<StopDto> stopDtos = stops.stream()
                .map(s -> new StopDto(s.getStopId(), s.getStopName(), s.getStopLat(), s.getStopLon()))
                .toList();
        return new AdminRouteDto(route.getRouteId(), route.getRouteShortName(), route.getRouteLongName(), stopDtos);
    }
}
