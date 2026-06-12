package io.github.sidneyroberto9.spring_session_lite.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionUser {
    private final String userId;
    private final String email;
    private final String sessionId;
}
