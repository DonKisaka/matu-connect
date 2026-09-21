package com.matuconnect.report;


/**
 * One row of the most-searched-routes report: an origin/destination pair and
 * how many times it has been looked up.
 * <p>
 * Stop names are carried alongside the ids because the report is read by
 * people — an administrator comparing "Kencom/Ambassadeur to Westlands
 * Terminal" against "0411MEB to 0213WTD" is doing the resolution in their
 * head otherwise. {@code searchCount} is a long because it comes straight
 * from a SQL {@code count()}.
 */
public record PopularRoute(
        String originStopId,
        String originStopName,
        String destinationStopId,
        String destinationStopName,
        long searchCount,
        long foundCount
) {

    /**
     * Share of searches for this pair that actually yielded a route, as a
     * percentage. A popular pair with a low success rate is a concrete,
     * demand-weighted coverage gap.
     */
    public int successRatePercent() {
        return searchCount == 0 ? 0 : (int) Math.round((foundCount * 100.0) / searchCount);
    }
}
