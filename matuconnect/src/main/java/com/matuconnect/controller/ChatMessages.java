package com.matuconnect.controller;


import java.util.List;

/**
 * Request/response shapes for the chat endpoint. Package-private (used
 * only by ChatController within this package) — unlike StopDto,
 * RouteAdviceDto, and CoverageDto, these don't need to be public, so
 * sharing one file is legal (Java's one-public-type-per-file rule only
 * applies to public types).
 */

/**
 * One earlier turn of the conversation, replayed so the agent can resolve
 * references like "the first one" against what it previously said.
 *
 * @param role    "user" or "assistant"; anything else is ignored
 * @param content the text of that turn
 */
record ChatTurn(String role, String content) {
}

/**
 * The agent holds no server-side session, so the client sends whatever
 * prior turns it wants taken into account alongside the new message.
 * {@code history} may be null or empty, which is treated as a fresh
 * conversation.
 */
record ChatRequest(String message, List<ChatTurn> history) {
}

record ChatResponse(String reply) {
}
