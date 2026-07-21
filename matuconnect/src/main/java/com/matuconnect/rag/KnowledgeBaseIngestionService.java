package com.matuconnect.rag;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the supplementary Nairobi transit knowledge-base documents into
 * the pgvector store, so {@code QuestionAnswerAdvisor} has something to
 * retrieve against.
 * <p>
 * Plain {@link TextReader} is used rather than a dedicated Markdown
 * reader (which would need the separate spring-ai-markdown-document-
 * reader dependency) — these documents are prose with light markdown
 * formatting, not structured data the model needs to navigate by
 * heading/section metadata, so the extra dependency wasn't worth it for
 * three files.
 * <p>
 * Chunking uses {@link TokenTextSplitter} with its default settings —
 * fixed-token-size chunks, not a custom section-aware splitter. Simpler
 * and standard, per the project's stated preference; the trade-off is a
 * chunk boundary could occasionally fall mid-section rather than
 * respecting a "##" heading, which the default splitter's
 * sentence-boundary awareness (Spring AI 2.0) mitigates but doesn't
 * fully eliminate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseIngestionService {

    private final VectorStore vectorStore;

    @Value("classpath:/knowledge-base/nairobi_knowledge_base.md")
    private Resource generalKnowledgeBaseResource;

    @Value("classpath:/knowledge-base/nairobi_route_updates_2024_2026.md")
    private Resource routeUpdatesResource;

    @Value("classpath:/knowledge-base/nairobi_expressway_matatu_operations_2026.md")
    private Resource expresswayOperationsResource;

    public void ingestAll() {
        List<Resource> sources = List.of(
                generalKnowledgeBaseResource,
                routeUpdatesResource,
                expresswayOperationsResource);
        TokenTextSplitter splitter = TokenTextSplitter.builder().build();

        List<Document> allChunks = new ArrayList<>();

        for (Resource source : sources) {
            TextReader textReader = new TextReader(source);

            List<Document> rawDocuments = textReader.get();
            List<Document> chunks = splitter.apply(rawDocuments);

            allChunks.addAll(chunks);
            log.info("Split {} into {} chunks.", source.getFilename(), chunks.size());
        }

        vectorStore.add(allChunks);
        log.info("Knowledge base ingestion complete: {} total chunks embedded and stored.", allChunks.size());
    }
}
