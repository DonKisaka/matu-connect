package com.matuconnect.controller;


import com.matuconnect.graph.CoverageAnalysisService;
import com.matuconnect.graph.CoverageGapResult;
import com.matuconnect.model.Role;
import com.matuconnect.report.PopularRoute;
import com.matuconnect.repository.JourneySearchRepository;
import com.matuconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reporting endpoints backing the administrator dashboard.
 * <p>
 * Two of the three reports are demand-driven — they describe what people
 * actually searched for — while the coverage report is structural, derived
 * from the graph alone. Read together they answer different questions: the
 * graph says which stops are unreachable, the search log says which
 * unreachable journeys anyone actually wanted.
 * <p>
 * Not yet access-controlled. These become {@code ADMIN}-only once Spring
 * Security is wired in; the paths are already namespaced under
 * {@code /api/reports} so that gating is a single matcher rather than a
 * scattering of annotations.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final int DEFAULT_REPORT_LIMIT = 10;
    private static final int MAX_REPORT_LIMIT = 100;

    private final JourneySearchRepository journeySearchRepository;
    private final UserRepository userRepository;
    private final CoverageAnalysisService coverageAnalysisService;

    /**
     * Most-searched origin/destination pairs. Each row also carries how often
     * that pair actually produced a route, so a heavily requested journey the
     * network cannot serve stands out rather than merely ranking highly.
     */
    @GetMapping("/popular-routes")
    public List<PopularRouteDto> popularRoutes(
            @RequestParam(defaultValue = "" + DEFAULT_REPORT_LIMIT) int limit) {

        int capped = Math.clamp(limit, 1, MAX_REPORT_LIMIT);
        return journeySearchRepository.findMostSearchedRoutes(PageRequest.of(0, capped)).stream()
                .map(PopularRouteDto::from)
                .toList();
    }

    /** Headline counts for the administrator dashboard. */
    @GetMapping("/usage")
    public UsageStatsDto usage() {
        long totalSearches = journeySearchRepository.count();
        long successfulSearches = journeySearchRepository.countByRouteFound(true);
        CoverageGapResult coverage = coverageAnalysisService.analyzeCoverage();

        return new UsageStatsDto(
                userRepository.count(),
                userRepository.countByRole(Role.ADMIN),
                totalSearches,
                successfulSearches,
                totalSearches - successfulSearches,
                coverage.totalStops(),
                coverage.mainComponentSize(),
                coverage.isolatedClusters().size());
    }
}
