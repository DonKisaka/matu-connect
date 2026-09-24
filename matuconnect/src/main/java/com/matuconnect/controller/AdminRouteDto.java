package com.matuconnect.controller;


import java.util.List;

/** REST-facing shape for a route managed through the admin dashboard. */
public record AdminRouteDto(
        String routeId,
        String routeShortName,
        String routeLongName,
        List<StopDto> stopsInOrder
) {
}
