package io.github.sidneyroberto9.spring_session_lite.security;

import java.util.List;

/**
 * Immutable authenticated principal exposed by the library. Populated by the
 * {@link SpringSessionLiteAuthenticationFilter} and injectable via
 * {@code @SpringSessionLiteCurrentSession}.
 */
public record SpringSessionLiteUser(String userId, String email, String sessionId, List<String> roles) {

    public SpringSessionLiteUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public SpringSessionLiteUser(String userId, String email, String sessionId) {
        this(userId, email, sessionId, List.of());
    }
}
