package com.matuconnect.graph;


import lombok.RequiredArgsConstructor;
import org.jgrapht.Graph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the matatu network graph once, at application startup, and
 * exposes it as a singleton bean. Downstream consumers (RoutingService,
 * CoverageAnalysisService, and eventually the Spring AI agent's tools)
 * inject {@code Graph<String, MatatuEdge>} directly rather than each
 * re-querying the database and rebuilding the graph themselves.
 * <p>
 * Trade-off worth noting: this means the graph is a snapshot taken at
 * startup. If GTFS data changes at runtime (e.g. a re-ingestion via
 * {@code gtfs.ingestion.force=true} on a running app), the graph bean
 * will not reflect it until the application restarts. That's acceptable
 * for this project's scope — the graph isn't expected to change between
 * a demo run and the next.
 */
@Configuration
@RequiredArgsConstructor
public class MatatuGraphConfig {

    private final MatatuGraphBuilder matatuGraphBuilder;

    @Bean
    public Graph<String, MatatuEdge> matatuGraph() {
        return matatuGraphBuilder.buildGraph();
    }
}
