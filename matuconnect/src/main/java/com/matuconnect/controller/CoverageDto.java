package com.matuconnect.controller;


import java.util.List;

/**
 * REST-facing shape for a coverage gap summary.
 */
public record CoverageDto(
        int totalStops,
        int mainNetworkSize,
        int isolatedClusterCount,
        List<StopDto> exampleIsolatedStops,
        List<StopDto> worstServedStops
) {
}