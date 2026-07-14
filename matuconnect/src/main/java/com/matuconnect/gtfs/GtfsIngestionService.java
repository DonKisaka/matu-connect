package com.matuconnect.gtfs;



import com.matuconnect.model.*;
import com.matuconnect.repository.*;
import com.opencsv.CSVReaderHeaderAware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the Digital Matatus GTFS feed from {@code src/main/resources/gtfs/}
 * and persists it via the 5 core repositories, in dependency order:
 * Stops -&gt; Routes -&gt; Trips -&gt; StopTimes -&gt; Shapes.
 * <p>
 * agency.txt, calendar.txt, calendar_dates.txt, feed_info.txt and
 * frequencies.txt are intentionally not ingested — see project notes for
 * the scoping rationale (single static agency/service pattern, no
 * headway-based trips in this feed).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GtfsIngestionService {

    private static final int BATCH_SIZE = 500;

    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;
    private final ShapePointRepository shapePointRepository;

    @Transactional
    public void ingestAll() {
        log.info("Starting GTFS ingestion...");

        var stopsById = ingestStops();
        log.info("Ingested {} stops", stopsById.size());

        var routesById = ingestRoutes();
        log.info("Ingested {} routes", routesById.size());

        var tripsById = ingestTrips(routesById);
        log.info("Ingested {} trips", tripsById.size());

        var stopTimeCount = ingestStopTimes(tripsById, stopsById);
        log.info("Ingested {} stop_times rows", stopTimeCount);

        var shapePointCount = ingestShapes();
        log.info("Ingested {} shape points", shapePointCount);

        log.info("GTFS ingestion complete.");
    }

    // ---------------------------------------------------------------
    // stops.txt
    // ---------------------------------------------------------------

    private Map<String, Stop> ingestStops() {
        record RawStop(String stopId, String stopName, double lat, double lon,
                       Integer locationType, String parentStationId) {
        }

        var rawStops = new ArrayList<RawStop>();

        try (var reader = new CSVReaderHeaderAware(new InputStreamReader(
                new ClassPathResource("gtfs/stops.txt").getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                var locationTypeRaw = row.get("location_type");
                Integer locationType = isBlank(locationTypeRaw) ? null : Integer.parseInt(locationTypeRaw.trim());

                rawStops.add(new RawStop(
                        row.get("stop_id").trim(),
                        row.get("stop_name"),
                        Double.parseDouble(row.get("stop_lat").trim()),
                        Double.parseDouble(row.get("stop_lon").trim()),
                        locationType,
                        trimToNull(row.get("parent_station"))
                ));
            }
        } catch (Exception e) {
            throw new GtfsIngestionException("Failed to read stops.txt", e);
        }

        // Pass 1: insert every stop with parentStation left null. Some rows
        // reference a parent that appears LATER in the file, so we can't
        // safely wire up the self-referencing FK in a single pass.
        var stopsById = new HashMap<String, Stop>(rawStops.size());
        var batch = new ArrayList<Stop>(BATCH_SIZE);
        for (var raw : rawStops) {
            var stop = new Stop();
            stop.setStopId(raw.stopId());
            stop.setStopName(raw.stopName());
            stop.setStopLat(raw.lat());
            stop.setStopLon(raw.lon());
            stop.setLocationType(raw.locationType());
            stopsById.put(raw.stopId(), stop);
            batch.add(stop);
            if (batch.size() == BATCH_SIZE) {
                stopRepository.saveAll(batch);
                batch.clear();
            }
        }
        flushRemaining(batch, stopRepository::saveAll);

        // Pass 2: now that every stop exists in memory, wire up parents.
        var updates = new ArrayList<Stop>(BATCH_SIZE);
        for (var raw : rawStops) {
            if (raw.parentStationId() == null) {
                continue;
            }
            var parent = stopsById.get(raw.parentStationId());
            if (parent == null) {
                log.warn("Stop {} references unknown parent_station {} — skipping link",
                        raw.stopId(), raw.parentStationId());
                continue;
            }
            var child = stopsById.get(raw.stopId());
            child.setParentStation(parent);
            updates.add(child);
            if (updates.size() == BATCH_SIZE) {
                stopRepository.saveAll(updates);
                updates.clear();
            }
        }
        flushRemaining(updates, stopRepository::saveAll);

        return stopsById;
    }

    // ---------------------------------------------------------------
    // routes.txt
    // ---------------------------------------------------------------

    private Map<String, Route> ingestRoutes() {
        var routesById = new HashMap<String, Route>();
        var batch = new ArrayList<Route>(BATCH_SIZE);

        try (var reader = new CSVReaderHeaderAware(new InputStreamReader(
                new ClassPathResource("gtfs/routes.txt").getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                var route = new Route();
                route.setRouteId(row.get("route_id").trim());
                route.setAgencyId(row.get("agency_id"));
                route.setRouteShortName(trimToNull(row.get("route_short_name")));
                route.setRouteLongName(row.get("route_long_name"));

                var routeTypeRaw = row.get("route_type");
                route.setRouteType(isBlank(routeTypeRaw) ? null : Integer.parseInt(routeTypeRaw.trim()));

                routesById.put(route.getRouteId(), route);
                batch.add(route);
                if (batch.size() == BATCH_SIZE) {
                    routeRepository.saveAll(batch);
                    batch.clear();
                }
            }
        } catch (Exception e) {
            throw new GtfsIngestionException("Failed to read routes.txt", e);
        }

        flushRemaining(batch, routeRepository::saveAll);
        return routesById;
    }

    // ---------------------------------------------------------------
    // trips.txt
    // ---------------------------------------------------------------

    private Map<String, Trip> ingestTrips(Map<String, Route> routesById) {
        var tripsById = new HashMap<String, Trip>();
        var batch = new ArrayList<Trip>(BATCH_SIZE);

        try (var reader = new CSVReaderHeaderAware(new InputStreamReader(
                new ClassPathResource("gtfs/trips.txt").getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                var routeId = row.get("route_id").trim();
                var route = routesById.get(routeId);
                if (route == null) {
                    log.warn("Trip {} references unknown route {} — skipping", row.get("trip_id"), routeId);
                    continue;
                }

                var trip = new Trip();
                trip.setTripId(row.get("trip_id").trim());
                trip.setRoute(route);
                trip.setServiceId(row.get("service_id"));
                trip.setTripHeadsign(row.get("trip_headsign"));

                var directionRaw = row.get("direction_id");
                trip.setDirectionId(isBlank(directionRaw) ? null : Integer.parseInt(directionRaw.trim()));
                trip.setShapeId(trimToNull(row.get("shape_id")));

                tripsById.put(trip.getTripId(), trip);
                batch.add(trip);
                if (batch.size() == BATCH_SIZE) {
                    tripRepository.saveAll(batch);
                    batch.clear();
                }
            }
        } catch (Exception e) {
            throw new GtfsIngestionException("Failed to read trips.txt", e);
        }

        flushRemaining(batch, tripRepository::saveAll);
        return tripsById;
    }

    // ---------------------------------------------------------------
    // stop_times.txt
    // ---------------------------------------------------------------

    private int ingestStopTimes(Map<String, Trip> tripsById, Map<String, Stop> stopsById) {
        var batch = new ArrayList<StopTime>(BATCH_SIZE);
        var count = 0;

        try (var reader = new CSVReaderHeaderAware(new InputStreamReader(
                new ClassPathResource("gtfs/stop_times.txt").getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                var tripId = row.get("trip_id").trim();
                var stopId = row.get("stop_id").trim();

                var trip = tripsById.get(tripId);
                var stop = stopsById.get(stopId);
                if (trip == null || stop == null) {
                    log.warn("stop_times row skipped — trip={} stop={} (missing reference)", tripId, stopId);
                    continue;
                }

                var stopSequence = Integer.parseInt(row.get("stop_sequence").trim());

                var stopTime = new StopTime();
                stopTime.setId(new StopTimeId(tripId, stopSequence));
                stopTime.setTrip(trip);
                stopTime.setStop(stop);
                stopTime.setArrivalTime(GtfsTimeUtils.toSecondsSinceMidnight(row.get("arrival_time")));
                stopTime.setDepartureTime(GtfsTimeUtils.toSecondsSinceMidnight(row.get("departure_time")));

                batch.add(stopTime);
                count++;
                if (batch.size() == BATCH_SIZE) {
                    stopTimeRepository.saveAll(batch);
                    batch.clear();
                }
            }
        } catch (Exception e) {
            throw new GtfsIngestionException("Failed to read stop_times.txt", e);
        }

        flushRemaining(batch, stopTimeRepository::saveAll);
        return count;
    }

    // ---------------------------------------------------------------
    // shapes.txt
    // ---------------------------------------------------------------

    private int ingestShapes() {
        var batch = new ArrayList<ShapePoint>(BATCH_SIZE);
        var count = 0;

        try (var reader = new CSVReaderHeaderAware(new InputStreamReader(
                new ClassPathResource("gtfs/shapes.txt").getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                var shapeId = row.get("shape_id").trim();
                var sequence = Integer.parseInt(row.get("shape_pt_sequence").trim());

                var shapePoint = new ShapePoint();
                shapePoint.setId(new ShapePointId(shapeId, sequence));
                shapePoint.setShapePtLat(Double.parseDouble(row.get("shape_pt_lat").trim()));
                shapePoint.setShapePtLon(Double.parseDouble(row.get("shape_pt_lon").trim()));

                batch.add(shapePoint);
                count++;
                if (batch.size() == BATCH_SIZE) {
                    shapePointRepository.saveAll(batch);
                    batch.clear();
                }
            }
        } catch (Exception e) {
            throw new GtfsIngestionException("Failed to read shapes.txt", e);
        }

        flushRemaining(batch, shapePointRepository::saveAll);
        return count;
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static <T> void flushRemaining(List<T> batch, java.util.function.Consumer<List<T>> saveAll) {
        if (!batch.isEmpty()) {
            saveAll.accept(batch);
        }
    }
}