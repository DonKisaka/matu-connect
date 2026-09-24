package com.matuconnect.controller;


import com.matuconnect.admin.RouteAdminException;
import com.matuconnect.admin.RouteAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lets an administrator add, edit and remove matatu routes without a full
 * GTFS re-ingestion — see {@link RouteAdminService} for what this
 * deliberately does and doesn't cover. Reachable only by an administrator;
 * see {@code SecurityConfig}'s {@code /api/admin/**} rule.
 * <p>
 * None of this reaches the route planner until {@link #rebuild()} is
 * called — an edit is saved immediately, but only takes effect on the live
 * network graph as an explicit, separate publishing step.
 */
@RestController
@RequestMapping("/api/admin/routes")
@RequiredArgsConstructor
public class RouteAdminController {

    private final RouteAdminService routeAdminService;

    @GetMapping
    public List<AdminRouteDto> list() {
        return routeAdminService.listAdminRoutes();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RouteEditRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(routeAdminService.createRoute(request));
        } catch (RouteAdminException e) {
            return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<?> update(@PathVariable String routeId, @RequestBody RouteEditRequest request) {
        try {
            return ResponseEntity.ok(routeAdminService.updateRoute(routeId, request));
        } catch (RouteAdminException e) {
            return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
        }
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<?> delete(@PathVariable String routeId) {
        try {
            routeAdminService.deleteRoute(routeId);
            return ResponseEntity.noContent().build();
        } catch (RouteAdminException e) {
            return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
        }
    }

    /**
     * Rebuilds the in-memory routing graph from the database. Separate from
     * create/update/delete deliberately — see {@code MatatuGraphHolder}.
     */
    @PostMapping("/rebuild")
    public ResponseEntity<Void> rebuild() {
        routeAdminService.rebuildNetwork();
        return ResponseEntity.noContent().build();
    }
}
