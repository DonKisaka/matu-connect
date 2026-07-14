package com.matuconnect.model;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for StopTime: a trip visits many stops,
 * each identified uniquely by (trip_id, stop_sequence).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StopTimeId implements Serializable {

    private String tripId;

    private Integer stopSequence;
}