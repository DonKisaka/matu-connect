package com.matuconnect.controller;


import java.util.List;

/**
 * REST-facing shape for the walking-distance coverage analysis. Each
 * {@code GapPointDto} is a sampled grid point farther than the threshold
 * from any stop, not a stop itself — the frontend draws these as small
 * markers rather than reusing the stop-marker rendering.
 */
public record WalkingDistanceGapDto(
        List<GapPointDto> gaps,
        int gridPointsSampled,
        double thresholdMetres
) {
    public record GapPointDto(double latitude, double longitude, double nearestStopMetres) {
    }
}
