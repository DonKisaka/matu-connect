package com.matuconnect.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents one point along a route's physical path (GTFS shapes.txt).
 * Used to draw the actual road-following route line on the map
 * (as opposed to straight lines between stops).
 */
@Entity
@Table(name = "shapes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShapePoint {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "shapeId", column = @Column(name = "shape_id")),
            @AttributeOverride(name = "shapePtSequence", column = @Column(name = "shape_pt_sequence"))
    })
    private ShapePointId id;

    @Column(name = "shape_pt_lat", nullable = false)
    private Double shapePtLat;

    @Column(name = "shape_pt_lon", nullable = false)
    private Double shapePtLon;
}