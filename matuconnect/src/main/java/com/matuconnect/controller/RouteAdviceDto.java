package com.matuconnect.controller;


import java.util.List;

/**
 * REST-facing shape for a route suggestion, returned directly to the
 * frontend map — no LLM involved in this path.
 */
public record RouteAdviceDto(
        boolean routeFound,
        List<String> stopNamesInOrder,
        List<String> routeNamesUsed,
        int estimatedRideMinutes,
        int transferCount
) {
}