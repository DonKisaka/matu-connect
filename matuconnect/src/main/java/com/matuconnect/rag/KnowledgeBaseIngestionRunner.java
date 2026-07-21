package com.matuconnect.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Triggers {@link KnowledgeBaseIngestionService} once on startup, guarded
 * the same way {@code GtfsIngestionRunner} guards GTFS ingestion —
 * disable-able via a property, and skipped if already populated.
 * <p>
 * One difference from the GTFS runner: {@link VectorStore} has no
 * {@code count()} method, since it's a generic abstraction over very
 * different backing stores. "Already populated" is instead checked with
 * a cheap similarity search — if it returns anything at all, ingestion
 * is assumed to have already run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseIngestionRunner implements CommandLineRunner {

    private final KnowledgeBaseIngestionService knowledgeBaseIngestionService;
    private final VectorStore vectorStore;

    @Value("${matuconnect.kb.ingestion.enabled:true}")
    private boolean ingestionEnabled;

    @Value("${matuconnect.kb.ingestion.force:false}")
    private boolean forceReingest;

    @Override
    public void run(String... args) {
        if (!ingestionEnabled) {
            log.info("Knowledge base ingestion disabled (matuconnect.kb.ingestion.enabled=false) — skipping.");
            return;
        }

        if (!forceReingest && alreadyPopulated()) {
            log.info("Vector store already contains knowledge base content — skipping ingestion. " +
                    "Set matuconnect.kb.ingestion.force=true to re-run.");
            return;
        }

        knowledgeBaseIngestionService.ingestAll();
    }

    private boolean alreadyPopulated() {
        List<Document> probe = vectorStore.similaritySearch(
                SearchRequest.builder().query("Nairobi matatu").topK(1).build());
        return probe != null && !probe.isEmpty();
    }
}