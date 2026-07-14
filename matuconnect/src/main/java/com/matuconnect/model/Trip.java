package com.matuconnect.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single directional run of a route (GTFS trips.txt).
 * Each Route typically has two Trips — one per direction_id (0/1).
 */
@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @Column(name = "trip_id", length = 50)
    private String tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "service_id", length = 50)
    private String serviceId;

    @Column(name = "trip_headsign", length = 255)
    private String tripHeadsign;

    /**
     * 0 = one direction, 1 = the opposite direction along the same route.
     */
    @Column(name = "direction_id")
    private Integer directionId;

    /**
     * Links to the geometry (path) for this trip in shapes.txt.
     * Not a hard FK here since ShapePoint uses a composite key —
     * joined manually in the ingestion/graph service instead.
     */
    @Column(name = "shape_id", length = 50)
    private String shapeId;
}