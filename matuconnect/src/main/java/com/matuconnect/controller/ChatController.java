package com.matuconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bridges the frontend chat UI to the configured MatuConnect chat agent.
 * The {@link ChatClient} bean already carries the system prompt, the
 * route-advisory tools, and the knowledge-base RAG advisor (see
 * {@code com.matuconnect.agent.MatuConnectChatConfig}); this controller
 * only forwards the user's message and returns the reply.
 *
 * <p>Blocking call — no streaming. Each request is independent; the
 * agent holds no per-user conversation memory.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
class ChatController {

    private final ChatClient chatClient;

    @PostMapping
    ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = chatClient.prompt()
                .user(request.message())
                .call()
                .content();
        return new ChatResponse(reply);
    }
}
