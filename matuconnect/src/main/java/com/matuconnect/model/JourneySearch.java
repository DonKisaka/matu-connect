package com.matuconnect.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One journey a user asked the system to plan, recorded whether or not a
 * route was found.
 * <p>
 * Failed searches are kept deliberately: a repeatedly requested pair that the
 * network cannot serve is precisely the kind of evidence a planner wants, and
 * discarding it would leave the coverage story dependent on graph structure
 * alone rather than on what people actually try to travel.
 * <p>
 * {@code user} is nullable so anonymous searches still count toward the
 * most-searched-routes report — gating that behind sign-up would bias the
 * data toward whoever happens to have an account.
 */
@Entity
@Table(name = "journey_searches", indexes = {
        @Index(name = "idx_journey_searches_user_id", columnList = "user_id"),
        @Index(name = "idx_journey_searches_searched_at", columnList = "searched_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JourneySearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for a search made while signed out. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_stop_id", nullable = false)
    private Stop originStop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_stop_id", nullable = false)
    private Stop destinationStop;

    @Column(name = "route_found", nullable = false)
    private boolean routeFound;

    /** Null when no route was found. */
    @Column(name = "transfer_count")
    private Integer transferCount;

    /** Null when no route was found. */
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;
}
