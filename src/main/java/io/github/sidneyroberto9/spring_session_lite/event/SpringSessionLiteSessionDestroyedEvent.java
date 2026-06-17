package io.github.sidneyroberto9.spring_session_lite.event;

/**
 * Published after a session is destroyed (logout or single-session revocation).
 */
public record SpringSessionLiteSessionDestroyedEvent(String sessionId) {
}
