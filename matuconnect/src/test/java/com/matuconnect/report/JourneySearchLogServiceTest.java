package com.matuconnect.report;

import com.matuconnect.graph.RouteResult;
import com.matuconnect.model.JourneySearch;
import com.matuconnect.model.Stop;
import com.matuconnect.repository.JourneySearchRepository;
import com.matuconnect.repository.StopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JourneySearchLogServiceTest {

    private JourneySearchRepository journeySearchRepository;
    private StopRepository stopRepository;
    private JourneySearchLogService service;

    private static Stop stop(String id, String name) {
        Stop s = new Stop();
        s.setStopId(id);
        s.setStopName(name);
        s.setStopLat(-1.28);
        s.setStopLon(36.82);
        return s;
    }

    @BeforeEach
    void setUp() {
        journeySearchRepository = mock(JourneySearchRepository.class);
        stopRepository = mock(StopRepository.class);
        service = new JourneySearchLogService(journeySearchRepository, stopRepository);

        when(stopRepository.findById("0110IRT")).thenReturn(Optional.of(stop("0110IRT", "Ngara")));
        when(stopRepository.findById("0113LMD")).thenReturn(Optional.of(stop("0113LMD", "Limuru Terminus")));
    }

    private JourneySearch captureSaved() {
        ArgumentCaptor<JourneySearch> captor = ArgumentCaptor.forClass(JourneySearch.class);
        verify(journeySearchRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordsRouteDetailsWhenAJourneyIsFound() {
        RouteResult found = new RouteResult(
                List.of("0110IRT", "0113LMD"), List.of("116"), 3960, 0);

        service.record("0110IRT", "0113LMD", Optional.of(found), null);

        JourneySearch saved = captureSaved();
        assertThat(saved.isRouteFound()).isTrue();
        assertThat(saved.getTransferCount()).isZero();
        assertThat(saved.getEstimatedMinutes()).isEqualTo(66); // 3960s rounded up
        assertThat(saved.getOriginStop().getStopId()).isEqualTo("0110IRT");
        assertThat(saved.getDestinationStop().getStopId()).isEqualTo("0113LMD");
        assertThat(saved.getSearchedAt()).isNotNull();
    }

    @Test
    void stillRecordsWhenNoRouteWasFound() {
        // A repeatedly requested pair the network cannot serve is the whole
        // point of keeping failed searches, so this must not be skipped.
        service.record("0110IRT", "0113LMD", Optional.empty(), null);

        JourneySearch saved = captureSaved();
        assertThat(saved.isRouteFound()).isFalse();
        assertThat(saved.getTransferCount()).isNull();
        assertThat(saved.getEstimatedMinutes()).isNull();
    }

    @Test
    void recordsAnonymousSearchesWithNoUser() {
        service.record("0110IRT", "0113LMD", Optional.empty(), null);

        assertThat(captureSaved().getUser()).isNull();
    }

    @Test
    void skipsSearchesAgainstStopsThatAreNotInTheFeed() {
        when(stopRepository.findById("NOPE")).thenReturn(Optional.empty());

        service.record("NOPE", "0113LMD", Optional.empty(), null);

        verify(journeySearchRepository, never()).save(any());
    }

    @Test
    void neverPropagatesAFailureToTheCaller() {
        // Logging is a side effect of answering a route query; a commuter must
        // still get their route if the write fails.
        when(journeySearchRepository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> service.record("0110IRT", "0113LMD", Optional.empty(), null))
                .doesNotThrowAnyException();
    }
}
