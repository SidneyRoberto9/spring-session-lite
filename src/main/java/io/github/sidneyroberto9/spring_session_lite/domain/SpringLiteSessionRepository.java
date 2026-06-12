package io.github.sidneyroberto9.spring_session_lite.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface SpringLiteSessionRepository extends JpaRepository<SpringLiteSession, String> {

    Optional<SpringLiteSession> findBySessionId(String sessionId);

    @Modifying
    @Transactional
    long deleteByExpiresAtBefore(Instant cutoff);
}
