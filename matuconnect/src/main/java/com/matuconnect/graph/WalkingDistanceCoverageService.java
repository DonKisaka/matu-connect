package com.matuconnect.graph;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Identifies areas of the city that are geographically far from any served
 * stop — a complement to {@link CoverageAnalysisService}'s graph-connectivity
 * analysis, which only sees stops that already exist in the network and says
 * nothing about the physical space between them.
 * <p>
 * Nairobi's stops are not evenly spread across a fixed area with known
 * population data, so this samples a regular grid over the bounding box of
 * every ingested stop and, for each grid point, finds the walking distance
 * (great-circle, via PostGIS) to the nearest stop. A point farther than the
 * configured threshold is a coverage gap — nobody standing there is within a
 * reasonable walk of a matatu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalkingDistanceCoverageService {

    /**
     * Degrees of latitude per metre, used to size the sampling grid. Treated
     * as constant across Nairobi's small extent — the ~0.03% difference
     * between latitude and longitude degree-length at this latitude doesn't
     * matter for a coverage-gap grid, only for survey-grade measurement.
     */
    private static final double METRES_PER_DEGREE = 111_320.0;

    /**
     * Defensive cap on grid dimensions. A pathological bounding box (e.g. a
     * single wildly out-of-range stop_lat/stop_lon from bad data) must not
     * turn one API call into millions of distance queries.
     */
    private static final int MAX_GRID_POINTS = 20_000;

    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Runs once at startup: PostGIS distance queries below use the KNN
     * (<->) operator, which is only fast with a matching GiST index. The
     * expression must exactly match the one used in {@link #nearestStopDistanceMetres}
     * for Postgres to recognise and use it.
     * <p>
     * Plain JDBC rather than the JPA {@link EntityManager} deliberately: a
     * {@code @PostConstruct} callback runs outside any Spring-managed
     * transaction and outside the AOP proxy, so a {@code @Transactional}
     * annotation on this method would silently never apply (the classic
     * self-invocation pitfall) — {@code executeUpdate()} on the
     * EntityManager would then fail with
     * {@code TransactionRequiredException}. A single DDL statement over a
     * plain JDBC connection needs no such transaction.
     */
    @jakarta.annotation.PostConstruct
    public void ensureSpatialIndex() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_stops_geog
                    ON stops
                    USING GIST ((ST_SetSRID(ST_MakePoint(stop_lon, stop_lat), 4326)::geography))
                    """);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to create spatial index on stops", e);
        }
    }

    public WalkingDistanceGapResult analyzeWalkingDistance(double thresholdMetres) {
        double[] bounds = boundingBox();
        if (bounds == null) {
            return new WalkingDistanceGapResult(List.of(), 0, thresholdMetres);
        }
        double minLat = bounds[0], maxLat = bounds[1], minLon = bounds[2], maxLon = bounds[3];

        double stepDeg = thresholdMetres / METRES_PER_DEGREE;
        int rows = Math.max(1, (int) Math.ceil((maxLat - minLat) / stepDeg));
        int cols = Math.max(1, (int) Math.ceil((maxLon - minLon) / stepDeg));

        if ((long) rows * cols > MAX_GRID_POINTS) {
            log.warn("Walking-distance grid would be {}x{} ({} points) — refusing, exceeds cap of {}.",
                    rows, cols, (long) rows * cols, MAX_GRID_POINTS);
            return new WalkingDistanceGapResult(List.of(), 0, thresholdMetres);
        }

        List<GapPoint> gaps = new ArrayList<>();
        int sampled = 0;
        for (int r = 0; r <= rows; r++) {
            double lat = minLat + r * stepDeg;
            for (int c = 0; c <= cols; c++) {
                double lon = minLon + c * stepDeg;
                sampled++;
                double distance = nearestStopDistanceMetres(lat, lon);
                if (distance > thresholdMetres) {
                    gaps.add(new GapPoint(lat, lon, distance));
                }
            }
        }

        log.info("Walking-distance coverage: sampled {} grid points ({}x{}), {} beyond {}m of any stop.",
                sampled, rows + 1, cols + 1, gaps.size(), thresholdMetres);

        return new WalkingDistanceGapResult(gaps, sampled, thresholdMetres);
    }

    @SuppressWarnings("unchecked")
    private double[] boundingBox() {
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT MIN(stop_lat), MAX(stop_lat), MIN(stop_lon), MAX(stop_lon) FROM stops"
        ).getResultList();
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            return null;
        }
        Object[] row = rows.get(0);
        return new double[]{
                ((Number) row[0]).doubleValue(), ((Number) row[1]).doubleValue(),
                ((Number) row[2]).doubleValue(), ((Number) row[3]).doubleValue(),
        };
    }

    private double nearestStopDistanceMetres(double lat, double lon) {
        Object result = entityManager.createNativeQuery("""
                SELECT ST_Distance(
                    (ST_SetSRID(ST_MakePoint(stop_lon, stop_lat), 4326)::geography),
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                )
                FROM stops
                ORDER BY (ST_SetSRID(ST_MakePoint(stop_lon, stop_lat), 4326)::geography)
                    <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                LIMIT 1
                """)
                .setParameter("lat", lat)
                .setParameter("lon", lon)
                .getSingleResult();
        return ((Number) result).doubleValue();
    }

    public record GapPoint(double latitude, double longitude, double nearestStopMetres) {
    }

    public record WalkingDistanceGapResult(List<GapPoint> gaps, int gridPointsSampled, double thresholdMetres) {
    }
}
