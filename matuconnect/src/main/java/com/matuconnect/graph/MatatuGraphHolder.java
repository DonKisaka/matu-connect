package com.matuconnect.graph;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the current matatu network graph behind a swappable reference.
 * <p>
 * The graph used to be a plain singleton bean, built once at startup and
 * never touched again — see the history in {@link MatatuGraphConfig}. That
 * stopped being true once an administrator could edit route data: a change
 * has to become routable without restarting the application. This holder is
 * the seam that makes that possible without every consumer needing to know
 * how or when a rebuild happens.
 * <p>
 * {@link #rebuild()} builds a brand-new graph first and only then swaps the
 * reference, so a request that reads {@link #get()} mid-rebuild still sees a
 * complete, consistent graph — either the old one or the new one, never a
 * half-populated one.
 */
@Slf4j
@Component
@DependsOn("gtfsIngestionRunner")
@RequiredArgsConstructor
public class MatatuGraphHolder {

    private final MatatuGraphBuilder matatuGraphBuilder;
    private final AtomicReference<Graph<String, MatatuEdge>> current =
            new AtomicReference<>();

    /**
     * Builds the initial graph at startup. {@code @DependsOn} guarantees the
     * GTFS feed is already in the database — see {@code GtfsIngestionRunner}'s
     * own Javadoc for why that ordering has to be declared explicitly rather
     * than assumed.
     */
    @PostConstruct
    void initialize() {
        current.set(matatuGraphBuilder.buildGraph());
    }

    public Graph<String, MatatuEdge> get() {
        return current.get();
    }

    /**
     * Rebuilds the graph from the database and atomically swaps it in.
     * Called after an administrator's route edit is saved — see
     * {@code RouteAdminService}.
     */
    public synchronized void rebuild() {
        Graph<String, MatatuEdge> fresh = matatuGraphBuilder.buildGraph();
        current.set(fresh);
        log.info("Matatu graph rebuilt: {} vertices, {} edges.",
                fresh.vertexSet().size(), fresh.edgeSet().size());
    }
}
