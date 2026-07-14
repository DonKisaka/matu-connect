package com.matuconnect.repository;


import com.matuconnect.model.ShapePoint;
import com.matuconnect.model.ShapePointId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShapePointRepository extends JpaRepository<ShapePoint, ShapePointId> {

    List<ShapePoint> findById_ShapeIdOrderById_ShapePtSequenceAsc(String shapeId);
}