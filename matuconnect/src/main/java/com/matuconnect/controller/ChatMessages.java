package com.matuconnect.controller;


/**
 * Request/response shapes for the chat endpoint. Package-private (used
 * only by ChatController within this package) — unlike StopDto,
 * RouteAdviceDto, and CoverageDto, these don't need to be public, so
 * sharing one file is legal (Java's one-public-type-per-file rule only
 * applies to public types).
 */
record ChatRequest(String message) {
}

record ChatResponse(String reply) {
}
