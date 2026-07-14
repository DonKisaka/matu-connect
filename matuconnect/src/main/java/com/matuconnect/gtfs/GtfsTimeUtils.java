package com.matuconnect.gtfs;


/**
 * Converts GTFS time strings ("H:MM:SS" / "HH:MM:SS") into seconds since
 * midnight of the service day.
 * <p>
 * GTFS deliberately permits values past 24:00:00 for trips that run into
 * the next calendar day (e.g. "25:30:00" = 1:30 AM the following morning).
 * java.time.LocalTime cannot represent this, so we store plain integer
 * seconds instead — this also makes duration math for graph edge weights
 * a simple subtraction.
 */
public final class GtfsTimeUtils {

    private GtfsTimeUtils() {
    }

    public static int toSecondsSinceMidnight(String gtfsTime) {
        if (gtfsTime == null || gtfsTime.isBlank()) {
            throw new IllegalArgumentException("GTFS time value is missing");
        }

        var parts = gtfsTime.trim().split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed GTFS time value: " + gtfsTime);
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        return hours * 3600 + minutes * 60 + seconds;
    }
}