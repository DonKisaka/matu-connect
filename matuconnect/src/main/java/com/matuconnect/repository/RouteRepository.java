package com.matuconnect.repository;


import com.matuconnect.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {

    /** Routes created through the admin editor — see RouteAdminService.ADMIN_ROUTE_PREFIX. */
    List<Route> findByRouteIdStartingWith(String prefix);
}
