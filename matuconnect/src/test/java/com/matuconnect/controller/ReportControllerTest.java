package com.matuconnect.controller;

import com.matuconnect.graph.CoverageAnalysisService;
import com.matuconnect.graph.CoverageGapResult;
import com.matuconnect.graph.IsolatedCluster;
import com.matuconnect.model.Role;
import com.matuconnect.report.PopularRoute;
import com.matuconnect.repository.JourneySearchRepository;
import com.matuconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JourneySearchRepository journeySearchRepository;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    CoverageAnalysisService coverageAnalysisService;

    @Test
    void popularRoutesReportsCountsAndSuccessRate() throws Exception {
        when(journeySearchRepository.findMostSearchedRoutes(any(Pageable.class))).thenReturn(List.of(
                new PopularRoute("0110IRT", "Ngara", "0113LMD", "Limuru Terminus", 8, 6),
                new PopularRoute("0411MEB", "Kencom/Ambassadeur", "0213WTD", "Westlands Terminal", 5, 0)));

        mockMvc.perform(get("/api/reports/popular-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originStopName").value("Ngara"))
                .andExpect(jsonPath("$[0].searchCount").value(8))
                .andExpect(jsonPath("$[0].successRatePercent").value(75))
                // A popular pair the network never serves is exactly what the
                // report exists to surface.
                .andExpect(jsonPath("$[1].searchCount").value(5))
                .andExpect(jsonPath("$[1].successRatePercent").value(0));
    }

    @Test
    void popularRoutesDefaultsToTenRows() throws Exception {
        when(journeySearchRepository.findMostSearchedRoutes(any(Pageable.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/popular-routes")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(journeySearchRepository).findMostSearchedRoutes(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void popularRoutesClampsAnAbsurdLimit() throws Exception {
        when(journeySearchRepository.findMostSearchedRoutes(any(Pageable.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/popular-routes").param("limit", "100000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(journeySearchRepository).findMostSearchedRoutes(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void usageCombinesAccountSearchAndNetworkFigures() throws Exception {
        when(userRepository.count()).thenReturn(12L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(journeySearchRepository.count()).thenReturn(40L);
        when(journeySearchRepository.countByRouteFound(true)).thenReturn(31L);
        when(coverageAnalysisService.analyzeCoverage()).thenReturn(
                new CoverageGapResult(4284, 3281,
                        List.of(new IsolatedCluster(List.of("0701KNK"))), List.of()));

        mockMvc.perform(get("/api/reports/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(12))
                .andExpect(jsonPath("$.adminUsers").value(2))
                .andExpect(jsonPath("$.totalSearches").value(40))
                .andExpect(jsonPath("$.successfulSearches").value(31))
                .andExpect(jsonPath("$.failedSearches").value(9))
                .andExpect(jsonPath("$.totalStops").value(4284))
                .andExpect(jsonPath("$.mainNetworkSize").value(3281))
                .andExpect(jsonPath("$.isolatedClusterCount").value(1));
    }
}
