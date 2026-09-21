package com.matuconnect.repository;


import com.matuconnect.model.JourneySearch;
import com.matuconnect.report.PopularRoute;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JourneySearchRepository extends JpaRepository<JourneySearch, Long> {

    /**
     * Most-searched origin/destination pairs, commonest first.
     * <p>
     * Grouped by stop id rather than stop name: several distinct stops share a
     * name in this feed ("Kencom/Ambassadeur" is three ids), and grouping by
     * name would merge journeys that begin at genuinely different stages.
     * The name is carried through the group-by purely so the projection can
     * report it.
     */
    @Query("""
            select new com.matuconnect.report.PopularRoute(
                o.stopId, o.stopName,
                d.stopId, d.stopName,
                count(js),
                sum(case when js.routeFound = true then 1L else 0L end))
            from JourneySearch js
              join js.originStop o
              join js.destinationStop d
            group by o.stopId, o.stopName, d.stopId, d.stopName
            order by count(js) desc, o.stopName asc
            """)
    List<PopularRoute> findMostSearchedRoutes(Pageable pageable);

    /** A single user's history, most recent first. */
    List<JourneySearch> findByUser_IdOrderBySearchedAtDesc(Long userId, Pageable pageable);

    long countByRouteFound(boolean routeFound);
}
