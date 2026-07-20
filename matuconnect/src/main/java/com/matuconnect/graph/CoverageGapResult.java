package com.matuconnect.graph;


import java.util.List;

/**
 * Combined result of a full coverage gap analysis run. See
 * {@link PoorlyServedStop} and {@link IsolatedCluster} — both split into
 * their own files (rather than nested here) because they need to be
 * public and a Java file may only have one public top-level type.
 */
public record CoverageGapResult(
        int totalStops,
        int mainComponentSize,
        List<IsolatedCluster> isolatedClusters,
        List<PoorlyServedStop> poorlyServedStops
) {
}