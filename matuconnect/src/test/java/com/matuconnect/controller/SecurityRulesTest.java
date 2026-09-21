package com.matuconnect.controller;

import org.springframework.context.annotation.Import;
import com.matuconnect.security.SecurityConfig;
import com.matuconnect.graph.CoverageAnalysisService;
import com.matuconnect.graph.CoverageGapResult;
import com.matuconnect.repository.JourneySearchRepository;
import com.matuconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The access rules in {@code SecurityConfig}, asserted directly.
 * <p>
 * These matter more than they look: an authorisation rule that silently stops
 * applying is invisible in manual testing, because the happy path — an admin
 * opening the dashboard — keeps working either way. The cases that must hold
 * are the refusals.
 */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JourneySearchRepository journeySearchRepository;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    CoverageAnalysisService coverageAnalysisService;

    private void stubEmptyReport() {
        when(journeySearchRepository.findMostSearchedRoutes(any(Pageable.class))).thenReturn(List.of());
        when(coverageAnalysisService.analyzeCoverage())
                .thenReturn(new CoverageGapResult(0, 0, List.of(), List.of()));
    }

    @Test
    @WithAnonymousUser
    void anonymousCallerIsRefusedTheReports() throws Exception {
        mockMvc.perform(get("/api/reports/popular-routes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "COMMUTER")
    void commuterIsRefusedTheReports() throws Exception {
        // Signed in but not privileged: 403, not 401 — the caller is known,
        // they simply may not have this.
        mockMvc.perform(get("/api/reports/popular-routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorIsAllowedTheReports() throws Exception {
        stubEmptyReport();

        mockMvc.perform(get("/api/reports/popular-routes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void anonymousCallerIsRefusedTheUsageDashboard() throws Exception {
        mockMvc.perform(get("/api/reports/usage"))
                .andExpect(status().isUnauthorized());
    }
}
