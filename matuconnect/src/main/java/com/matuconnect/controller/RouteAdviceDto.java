package com.matuconnect.controller;


import java.util.List;

/**
 * REST-facing shape for a route suggestion, returned directly to the
 * frontend map — no LLM involved in this path.
 * <p>
 * {@code stopsInOrder} carries coordinates alongside each name so the
 * frontend can draw the journey as a line on the map, not just list it as
 * text. It duplicates the names already in {@code stopNamesInOrder} rather
 * than replacing that field — existing callers of the plain name list stay
 * unaffected by an additive field. The line drawn this way connects real
 * stops straight-line, not the physical road geometry from the GTFS
 * shapes.txt data — that would need a separate endpoint over that table.
 */
public record RouteAdviceDto(
        boolean routeFound,
        List<String> stopNamesInOrder,
        List<StopDto> stopsInOrder,
        List<String> routeNamesUsed,
        int estimatedRideMinutes,
        int transferCount
) {
}