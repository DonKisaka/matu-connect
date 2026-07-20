package com.matuconnect.graph;


/**
 * A stop with unusually few connections in/out, relative to the rest of
 * the network — a candidate for being "poorly served."
 * <p>
 * Public (not package-private) because both {@code RouteAdvisoryTools}
 * (agent package, LLM-facing) and {@code CoverageController} (controller
 * package, REST-facing) need to read {@code stopId()} from instances
 * returned out of {@link CoverageAnalysisService}.
 */
public record PoorlyServedStop(String stopId, int inDegree, int outDegree) {

    public int totalDegree() {
        return inDegree + outDegree;
    }
}