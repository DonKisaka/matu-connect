package com.matuconnect.graph;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Identifies two distinct kinds of coverage gap in the matatu network:
 * <ol>
 *   <li><b>Isolated clusters</b> — groups of stops that are mutually
 *       reachable from each other (a strongly connected component) but
 *       are cut off from the network's main component. This is stronger
 *       than "no direct route" — it means no sequence of matatus, no
 *       matter how many transfers, connects that cluster to the rest of
 *       the city as modeled by this graph.</li>
 *   <li><b>Poorly-served stops</b> — stops with unusually few edges in
 *       or out relative to the rest of the network, even if they're
 *       technically part of the main component. A commuter there has
 *       very few route options.</li>
 * </ol>
 * Strongly connected components (not just weakly connected) were chosen
 * deliberately: because the graph is directed, a stop could have an
 * edge reaching it without any edge leaving it (or vice versa) — a real
 * asymmetric-service problem that weak connectivity would hide, since
 * weak connectivity ignores edge direction entirely.
 * <p>
 * Geographic "coverage deserts" (areas with no nearby stops at all,
 * independent of the graph) are intentionally out of scope here — that's
 * a spatial query better suited to the PostGIS extension already
 * installed in the database, and is noted as a natural extension for
 * future work rather than built into this graph-based analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverageAnalysisService {

    private final Graph<String, MatatuEdge> matatuGraph;

    /**
     * Stops in the bottom this-fraction of total degree (in-degree +
     * out-degree) are flagged as poorly served. Configurable since
     * "poorly served" is a modeling judgment, not a fixed physical fact —
     * default of 0.1 flags the bottom 10% of stops by connectivity.
     */
    @Value("${matuconnect.coverage.poorly-served-percentile:0.1}")
    private double poorlyServedPercentile;

    public CoverageGapResult analyzeCoverage() {
        List<IsolatedCluster> isolatedClusters = findIsolatedClusters();
        List<PoorlyServedStop> poorlyServedStops = findPoorlyServedStops();

        int totalStops = matatuGraph.vertexSet().size();
        int mainComponentSize = totalStops - isolatedClusters.stream()
                .mapToInt(cluster -> cluster.stopIds().size())
                .sum();

        log.info("Coverage analysis: {} total stops, main component {}, {} isolated clusters, {} poorly-served stops.",
                totalStops, mainComponentSize, isolatedClusters.size(), poorlyServedStops.size());

        return new CoverageGapResult(totalStops, mainComponentSize, isolatedClusters, poorlyServedStops);
    }

    /**
     * Finds every strongly connected component that is NOT the largest
     * one, and returns each as an {@link IsolatedCluster}. The single
     * largest component is treated as "the network" by convention — this
     * matches how the real matatu network behaves: one large mutually-
     * reachable core, with the rest being genuine gaps.
     */
    private List<IsolatedCluster> findIsolatedClusters() {
        KosarajuStrongConnectivityInspector<String, MatatuEdge> inspector =
                new KosarajuStrongConnectivityInspector<>(matatuGraph);

        List<Set<String>> components = inspector.stronglyConnectedSets();

        if (components.isEmpty()) {
            return List.of();
        }

        Set<String> mainComponent = components.stream()
                .max(Comparator.comparingInt(Set::size))
                .orElseThrow();

        return components.stream()
                .filter(component -> component != mainComponent)
                .map(component -> new IsolatedCluster(List.copyOf(component)))
                .sorted(Comparator.comparingInt((IsolatedCluster c) -> c.stopIds().size()).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Ranks every stop by total degree and returns the bottom
     * {@link #poorlyServedPercentile} fraction.
     */
    private List<PoorlyServedStop> findPoorlyServedStops() {
        List<PoorlyServedStop> allStops = matatuGraph.vertexSet().stream()
                .map(stopId -> new PoorlyServedStop(
                        stopId,
                        matatuGraph.inDegreeOf(stopId),
                        matatuGraph.outDegreeOf(stopId)))
                .sorted(Comparator.comparingInt(PoorlyServedStop::totalDegree))
                .collect(Collectors.toList());

        int cutoff = (int) Math.ceil(allStops.size() * poorlyServedPercentile);
        return allStops.subList(0, Math.min(cutoff, allStops.size()));
    }
}
