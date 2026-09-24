package com.matuconnect.admin;

import com.matuconnect.controller.AdminRouteDto;
import com.matuconnect.controller.RouteEditRequest;
import com.matuconnect.graph.MatatuGraphHolder;
import com.matuconnect.model.Stop;
import com.matuconnect.repository.StopRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the sequencing this feature exists for: a created route is saved
 * immediately but does not become routable until the network is explicitly
 * rebuilt — see {@link MatatuGraphHolder}.
 */
@SpringBootTest
class RouteAdminServiceTest {

    @Autowired
    RouteAdminService routeAdminService;

    @Autowired
    MatatuGraphHolder matatuGraphHolder;

    @Autowired
    StopRepository stopRepository;

    private String createdRouteId;

    @AfterEach
    void cleanUp() {
        if (createdRouteId != null) {
            routeAdminService.deleteRoute(createdRouteId);
            routeAdminService.rebuildNetwork();
            createdRouteId = null;
        }
    }

    @Test
    void aCreatedRouteOnlyBecomesRoutableAfterAnExplicitRebuild() {
        List<Stop> stops = stopRepository.findAll();
        Stop origin = stops.get(0);
        Stop destination = stops.get(1);

        int edgesBefore = matatuGraphHolder.get().edgeSet().size();

        AdminRouteDto created = routeAdminService.createRoute(new RouteEditRequest(
                "TEST99", "Test Route for RouteAdminServiceTest",
                List.of(
                        new RouteEditRequest.StopEntry(origin.getStopId(), null),
                        new RouteEditRequest.StopEntry(destination.getStopId(), 7))));
        createdRouteId = created.routeId();

        assertThat(created.routeId()).startsWith(RouteAdminService.ADMIN_ROUTE_PREFIX);
        assertThat(matatuGraphHolder.get().edgeSet().size())
                .as("graph must be unchanged until rebuild() is called")
                .isEqualTo(edgesBefore);

        routeAdminService.rebuildNetwork();

        assertThat(matatuGraphHolder.get().edgeSet().size())
                .as("rebuild must add exactly the one new edge for a 2-stop route")
                .isEqualTo(edgesBefore + 1);
        assertThat(matatuGraphHolder.get().containsEdge(origin.getStopId(), destination.getStopId())).isTrue();
    }

    @Test
    void updatingAPreExistingIngestedRouteIsRefused() {
        // The admin editor must never be able to touch real GTFS-ingested
        // route data through the update/delete path — only routes it
        // created itself, identified by the ADMIN- prefix.
        assertThatThrownBy(() -> routeAdminService.updateRoute("not-an-admin-route",
                new RouteEditRequest("X", "Y", List.of())))
                .isInstanceOf(RouteAdminException.class);
    }

    @Test
    void aRouteWithFewerThanTwoStopsIsRejected() {
        assertThatThrownBy(() -> routeAdminService.createRoute(new RouteEditRequest(
                "X", "Y", List.of(new RouteEditRequest.StopEntry("some-stop", null)))))
                .isInstanceOf(RouteAdminException.class);
    }
}
