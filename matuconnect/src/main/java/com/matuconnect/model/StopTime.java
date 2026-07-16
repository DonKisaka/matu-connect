package com.matuconnect.model;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents one scheduled stop-visit within a trip (GTFS stop_times.txt).
 * This is the largest table and the source of graph edges: consecutive
 * stop_times rows for the same trip (ordered by stop_sequence) become
 * edges between stops, weighted by travel duration.
 *
 * Times are stored as seconds-since-midnight (int) rather than LocalTime,
 * because GTFS permits times past 24:00:00 for trips crossing midnight
 * (e.g. 25:30:00), which LocalTime cannot represent. This also makes
 * duration/edge-weight calculations trivial (simple subtraction).
 */
@Entity
@Table(name = "stop_times", indexes = {
        @Index(name = "idx_stop_times_trip_id", columnList = "trip_id"),
        @Index(name = "idx_stop_times_stop_id", columnList = "stop_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StopTime {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "tripId", column = @Column(name = "trip_id")),
            @AttributeOverride(name = "stopSequence", column = @Column(name = "stop_sequence"))
    })
    private StopTimeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tripId")
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    /**
     * Seconds since midnight of the service day. May exceed 86400
     * (24:00:00) for post-midnight trips per GTFS spec.
     */
    @Column(name = "arrival_time", nullable = false)
    private Integer arrivalTime;

    @Column(name = "departure_time", nullable = false)
    private Integer departureTime;
}