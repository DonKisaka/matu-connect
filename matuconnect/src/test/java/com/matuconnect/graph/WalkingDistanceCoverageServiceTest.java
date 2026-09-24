package com.matuconnect.graph;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real PostGIS grid-sampling query against the test database —
 * this logic lives entirely in native SQL, so a unit test with a mocked
 * repository would not actually prove the query is correct.
 */
@SpringBootTest
class WalkingDistanceCoverageServiceTest {

    @Autowired
    WalkingDistanceCoverageService walkingDistanceCoverageService;

    @Test
    void aGenerousThresholdLeavesNoGaps() {
        // 35km clears even the feed's farthest-flung outlier stop (one real
        // stop sits ~20.3km from its nearest neighbour) — a grid point
        // beyond that of every stop at this spacing would indicate a real
        // bug in the bounding-box or distance math, not a genuine coverage
        // gap. The grid itself is coarse at this spacing, so this also
        // exercises the small-grid path distinct from the default-threshold
        // test below.
        var result = walkingDistanceCoverageService.analyzeWalkingDistance(35_000);

        assertThat(result.gridPointsSampled()).isGreaterThan(0);
        assertThat(result.gaps()).isEmpty();
        assertThat(result.thresholdMetres()).isEqualTo(35_000);
    }

    @Test
    void defaultThresholdSamplesARealGridWithSaneResults() {
        // 800m is the default used by the REST endpoint. This asserts the
        // grid was actually sampled at a realistic size (not silently
        // short-circuited by the pathological-input cap) and that every
        // gap reported is structurally sound.
        var result = walkingDistanceCoverageService.analyzeWalkingDistance(800);

        assertThat(result.gridPointsSampled()).isGreaterThan(100);
        assertThat(result.gaps().size()).isLessThanOrEqualTo(result.gridPointsSampled());
        result.gaps().forEach(gap -> assertThat(gap.nearestStopMetres()).isGreaterThan(800));
    }
}
