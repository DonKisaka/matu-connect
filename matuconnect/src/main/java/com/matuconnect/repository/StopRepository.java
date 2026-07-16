package com.matuconnect.repository;


import com.matuconnect.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, String> {

    List<Stop> findByStopNameContainingIgnoreCase(String partialName);
}