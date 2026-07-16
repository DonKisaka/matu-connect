package com.matuconnect.graph;


import java.util.List;

/**
 * A stop with unusually few connections in/out, relative to the rest of
 * the network — a candidate for being "poorly served."
 */
record PoorlyServedStop(String stopId, int inDegree, int outDegree) {

    int totalDegree() {
        return inDegree + outDegree;
    }
}

/**
 * A cluster of stops that are mutually reachable from each other
 * (a strongly connected component) but are NOT part of the network's
 * main component — i.e. a real coverage gap, not just a quiet corner
 * of an otherwise-connected network.
 */
record IsolatedCluster(List<String> stopIds) {
}

/**
 * Combined result of a full coverage gap analysis run.
 */
public record CoverageGapResult(
        int totalStops,
        int mainComponentSize,
        List<IsolatedCluster> isolatedClusters,
        List<PoorlyServedStop> poorlyServedStops
) {
}
