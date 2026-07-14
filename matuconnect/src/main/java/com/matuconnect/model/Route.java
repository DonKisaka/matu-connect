package com.matuconnect.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a matatu route/corridor (GTFS routes.txt).
 * route_type is consistently 3 (Bus) in the Digital Matatus dataset.
 */
@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @Column(name = "route_id", length = 50)
    private String routeId;

    @Column(name = "agency_id", length = 50)
    private String agencyId;

    @Column(name = "route_short_name", length = 50)
    private String routeShortName;

    /**
     * Often lists the named stops along the corridor, e.g.
     * "Odeon-Pangani-Roysambu-Githurai-KU". Useful as supplementary
     * text for the RAG knowledge base later.
     */
    @Column(name = "route_long_name", length = 500)
    private String routeLongName;

    /**
     * GTFS route_type: 3 = Bus (matatus are modeled as buses).
     */
    @Column(name = "route_type")
    private Integer routeType;
}
