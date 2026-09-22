package com.matuconnect.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridges the frontend chat UI to the configured MatuConnect chat agent.
 * The {@link ChatClient} bean already carries the system prompt, the
 * route-advisory tools, and the knowledge-base RAG advisor (see
 * {@code com.matuconnect.agent.MatuConnectChatConfig}); this controller
 * forwards the conversation and returns the reply.
 *
 * <p>Blocking call — no streaming. There is no server-side session: the
 * client replays whichever earlier turns it wants considered, which keeps
 * the backend stateless and horizontally scalable while still letting the
 * agent resolve back-references such as "the first one". Only the most
 * recent {@link #MAX_HISTORY_TURNS} turns are replayed, so a long-running
 * conversation cannot grow the prompt without bound.
 *
 * <p>The call to Claude retries once on failure. This environment has hit
 * intermittent TLS handshake failures reaching Anthropic's API — most likely
 * antivirus HTTPS inspection substituting a certificate the JVM's trust
 * store does not recognise — where one attempt fails and the very next
 * attempt, moments later, succeeds outright. That is the signature of a
 * transient per-connection fault, not a real outage, so a single retry on
 * a fresh connection is the correct response rather than surfacing it to
 * the user as a broken chat.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final int MAX_HISTORY_TURNS = 10;
    private static final int MAX_ATTEMPTS = 2;

    private final ChatClient chatClient;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        List<Message> conversation = toMessages(request);

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String reply = chatClient.prompt()
                        .messages(conversation)
                        .call()
                        .content();
                return new ChatResponse(reply);
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("Chat call attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.toString());
            }
        }
        throw lastFailure;
    }

    /**
     * Turns the replayed history plus the new message into the message list
     * the model sees. Unrecognised roles are dropped rather than guessed at —
     * the frontend also renders local-only "error" bubbles, which are UI
     * state and not something the agent ever said.
     */
    private static List<Message> toMessages(ChatRequest request) {
        List<Message> messages = new ArrayList<>();

        List<ChatTurn> history = request.history();
        if (history != null && !history.isEmpty()) {
            List<ChatTurn> recent = history.size() > MAX_HISTORY_TURNS
                    ? history.subList(history.size() - MAX_HISTORY_TURNS, history.size())
                    : history;

            for (ChatTurn turn : recent) {
                if (turn == null || turn.content() == null || turn.content().isBlank()) {
                    continue;
                }
                if ("user".equalsIgnoreCase(turn.role())) {
                    messages.add(new UserMessage(turn.content()));
                } else if ("assistant".equalsIgnoreCase(turn.role())) {
                    messages.add(new AssistantMessage(turn.content()));
                }
            }
        }

        messages.add(new UserMessage(request.message()));
        return messages;
    }
}
