package com.matuconnect.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single matatu stop or station (GTFS stops.txt).
 * Some stops reference a parent_station, forming a self-referencing
 * hierarchy (e.g., individual platforms under a shared terminus).
 */
@Entity
@Table(name = "stops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stop {

    @Id
    @Column(name = "stop_id", length = 50)
    private String stopId;

    @Column(name = "stop_name", nullable = false, length = 255)
    private String stopName;

    @Column(name = "stop_lat", nullable = false)
    private Double stopLat;

    @Column(name = "stop_lon", nullable = false)
    private Double stopLon;

    /**
     * GTFS location_type: 0 = stop/platform, 1 = station, blank/null = generic stop.
     */
    @Column(name = "location_type")
    private Integer locationType;

    /**
     * Self-referencing FK: some stops belong to a parent station.
     * Nullable — most stops have no parent.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_station", referencedColumnName = "stop_id")
    private Stop parentStation;
}
