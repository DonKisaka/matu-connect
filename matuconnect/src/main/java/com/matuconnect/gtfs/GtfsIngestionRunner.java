package com.matuconnect.gtfs;


import com.matuconnect.repository.StopRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Runs GTFS ingestion during bean initialisation rather than as a
 * {@code CommandLineRunner}.
 * <p>
 * This ordering is load-bearing, not stylistic. {@code MatatuGraphConfig}
 * builds the network graph from the database as an eager singleton during
 * context refresh, whereas CommandLineRunners only fire <em>after</em>
 * refresh completes. Ingesting from a runner therefore meant that, on a
 * fresh database, the graph was built from empty tables and stayed empty
 * for the whole session. Doing the work in {@code @PostConstruct} lets
 * {@code matatuGraph()} declare {@code @DependsOn("gtfsIngestionRunner")}
 * and be guaranteed populated data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GtfsIngestionRunner {

    private final GtfsIngestionService gtfsIngestionService;
    private final StopRepository stopRepository;

    @Value("${gtfs.ingestion.enabled:true}")
    private boolean ingestionEnabled;

    @Value("${gtfs.ingestion.force:false}")
    private boolean forceReingest;

    @PostConstruct
    public void run() {
        if (!ingestionEnabled) {
            log.info("GTFS ingestion disabled (gtfs.ingestion.enabled=false) — skipping.");
            return;
        }

        if (!forceReingest && stopRepository.count() > 0) {
            log.info("Stops table already populated ({} rows) — skipping GTFS ingestion. " +
                    "Set gtfs.ingestion.force=true to re-run.", stopRepository.count());
            return;
        }

        gtfsIngestionService.ingestAll();
    }
}
