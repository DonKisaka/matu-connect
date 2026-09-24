package com.matuconnect.controller;


import com.matuconnect.graph.CoverageAnalysisService;
import com.matuconnect.graph.CoverageGapResult;
import com.matuconnect.graph.WalkingDistanceCoverageService;
import com.matuconnect.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Direct coverage-gap endpoint for the map UI — e.g. to shade isolated
 * clusters or highlight poorly-served stops visually, without routing
 * the request through the chat agent.
 * <p>
 * Unlike the agent's {@code getCoverageGapSummary} tool (which caps
 * results at 5 example stops to stay small for an LLM context window),
 * this endpoint returns a slightly larger set (up to 20) since a map can
 * usefully render more markers than a chat response can usefully list.
 */
@RestController
@RequestMapping("/api/coverage")
@RequiredArgsConstructor
public class CoverageController {

    private static final int MAX_STOPS_FOR_MAP_DISPLAY = 20;

    /** ~10 minutes' walk, the default used when the caller doesn't override it. */
    private static final double DEFAULT_WALK_THRESHOLD_METRES = 800.0;

    private final CoverageAnalysisService coverageAnalysisService;
    private final WalkingDistanceCoverageService walkingDistanceCoverageService;
    private final StopRepository stopRepository;

    @GetMapping
    public CoverageDto coverageSummary() {
        CoverageGapResult result = coverageAnalysisService.analyzeCoverage();

        List<StopDto> isolatedStops = result.isolatedClusters().stream()
                .flatMap(cluster -> cluster.stopIds().stream())
                .limit(MAX_STOPS_FOR_MAP_DISPLAY)
                .map(this::resolveStop)
                .toList();

        List<StopDto> worstServed = result.poorlyServedStops().stream()
                .limit(MAX_STOPS_FOR_MAP_DISPLAY)
                .map(stop -> resolveStop(stop.stopId()))
                .toList();

        return new CoverageDto(
                result.totalStops(),
                result.mainComponentSize(),
                result.isolatedClusters().size(),
                isolatedStops,
                worstServed
        );
    }

    /**
     * Geographic complement to {@link #coverageSummary()} — areas far from
     * any stop, rather than stops poorly placed within the graph. See
     * {@link WalkingDistanceCoverageService} for why this is a sampled grid
     * rather than a per-stop or per-address analysis.
     */
    @GetMapping("/walking-distance")
    public WalkingDistanceGapDto walkingDistance(
            @RequestParam(required = false) Double thresholdMetres) {
        double threshold = thresholdMetres != null ? thresholdMetres : DEFAULT_WALK_THRESHOLD_METRES;
        WalkingDistanceCoverageService.WalkingDistanceGapResult result =
                walkingDistanceCoverageService.analyzeWalkingDistance(threshold);

        List<WalkingDistanceGapDto.GapPointDto> gaps = result.gaps().stream()
                .map(g -> new WalkingDistanceGapDto.GapPointDto(g.latitude(), g.longitude(), g.nearestStopMetres()))
                .toList();

        return new WalkingDistanceGapDto(gaps, result.gridPointsSampled(), result.thresholdMetres());
    }

    private StopDto resolveStop(String stopId) {
        return stopRepository.findById(stopId)
                .map(stop -> new StopDto(stop.getStopId(), stop.getStopName(), stop.getStopLat(), stop.getStopLon()))
                .orElse(new StopDto(stopId, stopId, 0.0, 0.0));
    }
}
