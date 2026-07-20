package com.matuconnect.controller;


import com.matuconnect.model.Stop;
import com.matuconnect.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Direct, deterministic access to stop data — no LLM involved. This is
 * what the Leaflet map hits to plot markers and to resolve a place name
 * the user typed into a search box into a stop_id, without paying for
 * (or waiting on) a chat round-trip for something that's really just a
 * database lookup.
 */
@RestController
@RequestMapping("/api/stops")
@RequiredArgsConstructor
public class StopController {

    private final StopRepository stopRepository;

    @GetMapping
    public List<StopDto> allStops() {
        return stopRepository.findAll().stream()
                .map(StopController::toDto)
                .toList();
    }

    @GetMapping("/search")
    public List<StopDto> searchStops(@RequestParam String query) {
        return stopRepository.findByStopNameContainingIgnoreCase(query).stream()
                .map(StopController::toDto)
                .toList();
    }

    private static StopDto toDto(Stop stop) {
        return new StopDto(stop.getStopId(), stop.getStopName(), stop.getStopLat(), stop.getStopLon());
    }
}
