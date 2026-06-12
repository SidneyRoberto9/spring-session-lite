package io.github.sidneyroberto9.spring_session_lite.service;

import io.github.sidneyroberto9.spring_session_lite.config.SpringSessionLiteProperties;
import io.github.sidneyroberto9.spring_session_lite.domain.Session;
import io.github.sidneyroberto9.spring_session_lite.domain.SessionRepository;
import io.github.sidneyroberto9.spring_session_lite.security.SessionUser;
import io.github.sidneyroberto9.spring_session_lite.util.NanoId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional
public class SessionLiteService {
    private final SpringSessionLiteProperties properties;
    private final SessionRepository repository;
    private final CookieManager cookieManager;
    private final IpResolver ipResolver;
    private final IpHasher ipHasher;

    public SessionUser login(String userId, String email, HttpServletRequest request, HttpServletResponse response) {
        Instant now = Instant.now();

        Session session = new Session();
        session.setSessionId(NanoId.generate(properties.getSessionIdLength()));
        session.setUserId(userId);
        session.setEmail(email);
        session.setIpHash(ipHasher.hash(ipResolver.resolve(request)));
        session.setCreatedAt(now);
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plus(properties.getTtl()));

        repository.save(session);
        cookieManager.write(response, session.getSessionId());

        return new SessionUser(userId, email, session.getSessionId());
    }

    @Transactional
    public Optional<SessionUser> validate(String sessionId, HttpServletRequest request) {
        return repository.findBySessionId(sessionId).flatMap(session -> {
            if (session.getExpiresAt().isBefore(Instant.now())) {
                return Optional.empty();
            }

            String currentIpHash = ipHasher.hash(ipResolver.resolve(request));

            if (!session.getIpHash().equals(currentIpHash)) {
                return Optional.empty();
            }

            session.setLastAccessedAt(Instant.now());
            return Optional.of(new SessionUser(session.getUserId(), session.getEmail(), session.getSessionId()));
        });
    }

    public void deleteExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }
}
