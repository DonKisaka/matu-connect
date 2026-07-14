package com.matuconnect.repository;


import com.matuconnect.model.StopTime;
import com.matuconnect.model.StopTimeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopTimeRepository extends JpaRepository<StopTime, StopTimeId> {

    /**
     * All stop_times for a trip, ordered by stop_sequence — this is what the
     * graph builder will use to derive consecutive-stop edges per trip.
     */
    List<StopTime> findByTrip_TripIdOrderById_StopSequenceAsc(String tripId);
}