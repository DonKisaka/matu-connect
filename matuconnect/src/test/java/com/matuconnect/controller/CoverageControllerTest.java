package com.matuconnect.controller;

import com.matuconnect.graph.CoverageAnalysisService;
import com.matuconnect.graph.CoverageGapResult;
import com.matuconnect.graph.WalkingDistanceCoverageService;
import com.matuconnect.repository.StopRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoverageController.class)
@Import(com.matuconnect.security.SecurityConfig.class)
class CoverageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CoverageAnalysisService coverageAnalysisService;

    @MockitoBean
    WalkingDistanceCoverageService walkingDistanceCoverageService;

    @MockitoBean
    StopRepository stopRepository;

    @Test
    @WithAnonymousUser
    void walkingDistanceIsReachableSignedOut() throws Exception {
        // Browsing the network, including the walking-distance overlay, must
        // not require an account — same rule as /api/coverage itself.
        when(coverageAnalysisService.analyzeCoverage())
                .thenReturn(new CoverageGapResult(0, 0, List.of(), List.of()));
        when(walkingDistanceCoverageService.analyzeWalkingDistance(800))
                .thenReturn(new WalkingDistanceCoverageService.WalkingDistanceGapResult(
                        List.of(new WalkingDistanceCoverageService.GapPoint(-1.3, 36.8, 950.0)),
                        120, 800));

        mockMvc.perform(get("/api/coverage/walking-distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gridPointsSampled").value(120))
                .andExpect(jsonPath("$.thresholdMetres").value(800))
                .andExpect(jsonPath("$.gaps[0].nearestStopMetres").value(950.0));
    }

    @Test
    @WithAnonymousUser
    void walkingDistanceHonoursAnExplicitThreshold() throws Exception {
        when(walkingDistanceCoverageService.analyzeWalkingDistance(500))
                .thenReturn(new WalkingDistanceCoverageService.WalkingDistanceGapResult(List.of(), 40, 500));

        mockMvc.perform(get("/api/coverage/walking-distance").param("thresholdMetres", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdMetres").value(500));
    }
}
