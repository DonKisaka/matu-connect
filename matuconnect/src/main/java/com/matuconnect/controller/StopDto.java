package com.matuconnect.controller;


/**
 * REST-facing shape for a stop. Deliberately separate from the agent
 * package's {@code StopSummary} record — that one is package-private and
 * scoped to what the LLM needs; this is the public contract the frontend
 * depends on, and the two shouldn't be coupled just because they
 * currently look similar.
 */
public record StopDto(String stopId, String stopName, double latitude, double longitude) {
}