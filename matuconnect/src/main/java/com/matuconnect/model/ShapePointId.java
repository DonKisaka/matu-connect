package com.matuconnect.model;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for ShapePoint: a shape is an ordered sequence
 * of lat/lon points, uniquely identified by (shape_id, shape_pt_sequence).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShapePointId implements Serializable {

    private String shapeId;

    private Integer shapePtSequence;
}