package io.github.sidneyroberto9.spring_session_lite.event;

import java.time.Instant;

/**
 * Published after a session is successfully created (login).
 */
public record SpringSessionLiteSessionCreatedEvent(String userId, String sessionId, Instant createdAt) {
}
