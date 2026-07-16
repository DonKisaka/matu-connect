package com.matuconnect.agent;



import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the MatuConnect chat agent using {@link ChatClientBuilderCustomizer} beans
 * rather than assembling everything in one large @Bean method.
 * <p>
 * Spring Boot's chat-client auto-configuration collects every
 * {@code ChatClientBuilderCustomizer} bean in the context and applies all of them,
 * in order, to each {@code ChatClient.Builder} it creates — so splitting
 * one concern per bean here doesn't cost any manual wiring. This buys:
 * <ul>
 *   <li><b>Separation of concerns</b> — the system prompt, the tool
 *       registration, and the RAG advisor can each be read and reasoned
 *       about on their own, without the other two as noise.</li>
 *   <li><b>Composability</b> — customizers stack. If a later part of the
 *       project needs, say, a logging advisor or a stricter chat-options
 *       customizer, it's a new small bean, not an edit to this one.</li>
 *   <li><b>Reuse</b> — if the project ever needs a second, differently
 *       -scoped ChatClient (e.g. an admin/debug client without the
 *       commuter-facing system prompt), these customizers don't have to
 *       be untangled from each other first.</li>
 * </ul>
 */
@Configuration
public class MatuConnectChatConfig {

    private static final String SYSTEM_PROMPT = """
            You are MatuConnect, a route advisory assistant for Nairobi's
            matatu network.

            Rules you must follow:
            1. Never guess a stop_id. Always call findStopsByName first to
               resolve a place name the user mentions into a real stop_id.
            2. If findStopsByName returns multiple plausible matches, ask
               the user to confirm which one they mean before calling
               suggestRoute — do not silently pick one.
            3. For point-to-point journey questions, use suggestRoute.
               For general questions about underserved areas or network
               gaps, use getCoverageGapSummary instead.
            4. For questions about route changes, termini, or Nairobi
               transit context (not point-to-point directions), rely on
               the retrieved knowledge-base context rather than your own
               prior knowledge — the underlying GTFS data is from
               2019-2020 and the knowledge base contains newer corrections.
            5. If a tool reports no route was found, say so plainly rather
               than inventing a plausible-sounding route.
            6. Keep answers concise and practical — state the routes to
               board, where to transfer if needed, and estimated time.
            """;

    @Bean
    public ChatClientBuilderCustomizer systemPromptCustomizer() {
        return builder -> builder.defaultSystem(SYSTEM_PROMPT);
    }

    @Bean
    public ChatClientBuilderCustomizer routeAdvisoryToolsCustomizer(RouteAdvisoryTools routeAdvisoryTools) {
        return builder -> builder.defaultTools(routeAdvisoryTools);
    }

    @Bean
    public ChatClientBuilderCustomizer knowledgeBaseRagCustomizer(VectorStore vectorStore) {
        QuestionAnswerAdvisor knowledgeBaseAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.5)
                        .topK(4)
                        .build())
                .build();

        return builder -> builder.defaultAdvisors(knowledgeBaseAdvisor);
    }

    /**
     * The ChatClient bean the rest of the app actually injects and calls.
     * By the time this method receives {@code chatClientBuilder}, every
     * ChatClientBuilderCustomizer bean above has already been applied to it —
     * this only needs to call build().
     */
    @Bean
    public ChatClient matuConnectChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}