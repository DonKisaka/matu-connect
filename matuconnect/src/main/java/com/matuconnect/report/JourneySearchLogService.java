package com.matuconnect.report;


import com.matuconnect.graph.RouteResult;
import com.matuconnect.model.JourneySearch;
import com.matuconnect.model.Stop;
import com.matuconnect.repository.JourneySearchRepository;
import com.matuconnect.repository.StopRepository;
import com.matuconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Records journey searches so the most-searched-routes report and a user's
 * personal history have something to draw on.
 * <p>
 * Recording is strictly a side effect of answering a route query and must
 * never change its outcome: a commuter should still get their route if the
 * write fails. Every failure is therefore swallowed and logged rather than
 * propagated, and the write runs in its own transaction
 * ({@link Propagation#REQUIRES_NEW}) so that a rollback here cannot mark a
 * surrounding transaction rollback-only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JourneySearchLogService {

    private final JourneySearchRepository journeySearchRepository;
    private final StopRepository stopRepository;
    private final UserRepository userRepository;

    /**
     * @param username the signed-in user, or null for an anonymous search.
     *                 Anonymous searches are still recorded — see
     *                 {@link JourneySearch}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String originStopId,
                       String destinationStopId,
                       Optional<RouteResult> result,
                       String username) {
        try {
            Stop origin = stopRepository.findById(originStopId).orElse(null);
            Stop destination = stopRepository.findById(destinationStopId).orElse(null);
            if (origin == null || destination == null) {
                // A search against a stop_id that isn't in the feed is not worth
                // a row; it says nothing about demand for a real journey.
                log.debug("Not recording search for unknown stop(s) {} -> {}",
                        originStopId, destinationStopId);
                return;
            }

            JourneySearch search = new JourneySearch();
            search.setOriginStop(origin);
            search.setDestinationStop(destination);
            search.setRouteFound(result.isPresent());
            search.setSearchedAt(Instant.now());
            result.ifPresent(route -> {
                search.setTransferCount(route.transferCount());
                search.setEstimatedMinutes((int) Math.ceil(route.totalTravelTimeSeconds() / 60.0));
            });

            // Signed-out searches are still recorded, just unattributed — see
            // the class comment. An unknown username is treated the same way
            // rather than dropping the row.
            if (username != null) {
                userRepository.findByUsername(username).ifPresent(search::setUser);
            }

            journeySearchRepository.save(search);
        } catch (RuntimeException e) {
            log.warn("Failed to record journey search {} -> {}: {}",
                    originStopId, destinationStopId, e.toString());
        }
    }
}
