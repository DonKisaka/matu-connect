package com.matuconnect.gtfs;


import com.matuconnect.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class GtfsIngestionRunner implements CommandLineRunner {

    private final GtfsIngestionService gtfsIngestionService;
    private final StopRepository stopRepository;

    @Value("${gtfs.ingestion.enabled:true}")
    private boolean ingestionEnabled;

    @Value("${gtfs.ingestion.force:false}")
    private boolean forceReingest;

    @Override
    public void run(String... args) {
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
