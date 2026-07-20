package com.matuconnect.graph;


import java.util.List;

/**
 * A cluster of stops that are mutually reachable from each other
 * (a strongly connected component) but are NOT part of the network's
 * main component — i.e. a real coverage gap, not just a quiet corner
 * of an otherwise-connected network.
 * <p>
 * Public for the same reason as {@link PoorlyServedStop} — read from
 * outside the graph package by both the agent's tools and the REST
 * controllers.
 */
public record IsolatedCluster(List<String> stopIds) {
}
