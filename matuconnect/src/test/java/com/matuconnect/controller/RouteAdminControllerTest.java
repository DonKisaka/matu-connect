package com.matuconnect.controller;

import com.matuconnect.admin.RouteAdminException;
import com.matuconnect.admin.RouteAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Same discipline as {@code SecurityRulesTest}: the refusals matter more
 * than the happy path, because a broken admin-only gate is invisible to
 * manual testing done as an admin.
 */
@WebMvcTest(RouteAdminController.class)
@Import(com.matuconnect.security.SecurityConfig.class)
class RouteAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RouteAdminService routeAdminService;

    @Test
    @WithAnonymousUser
    void anonymousCallerIsRefused() throws Exception {
        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "COMMUTER")
    void commuterIsRefused() throws Exception {
        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanListRoutes() throws Exception {
        when(routeAdminService.listAdminRoutes()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidCreateRequestReturnsBadRequestNotServerError() throws Exception {
        when(routeAdminService.createRoute(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RouteAdminException("A route needs at least 2 stops."));

        mockMvc.perform(post("/api/admin/routes")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeShortName\":\"X\",\"routeLongName\":\"Y\",\"stops\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rebuildIsAdminOnlyToo() throws Exception {
        mockMvc.perform(post("/api/admin/routes/rebuild")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "COMMUTER")
    void deleteIsRefusedForACommuter() throws Exception {
        mockMvc.perform(delete("/api/admin/routes/ADMIN-12345678")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }
}
