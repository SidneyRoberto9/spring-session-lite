package io.github.sidneyroberto9.spring_session_lite.service;

import io.github.sidneyroberto9.spring_session_lite.config.SpringSessionLiteProperties;
import io.github.sidneyroberto9.spring_session_lite.domain.SpringLiteSession;
import io.github.sidneyroberto9.spring_session_lite.domain.SpringLiteSessionRepository;
import io.github.sidneyroberto9.spring_session_lite.security.SpringSessionLiteUser;
import io.github.sidneyroberto9.spring_session_lite.util.NanoId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional
public class SpringSessionLiteService {
    private final SpringSessionLiteProperties properties;
    private final SpringLiteSessionRepository repository;
    private final SpringSessionLiteCookieManager cookieManager;
    private final SpringSessionLiteIpResolver ipResolver;
    private final SpringSessionLiteIpHasher ipHasher;

    public SpringSessionLiteUser login(String userId, String email, HttpServletRequest request, HttpServletResponse response) {
        Instant now = Instant.now();

        SpringLiteSession session = new SpringLiteSession();
        session.setSessionId(NanoId.generate(properties.getSessionIdLength()));
        session.setUserId(userId);
        session.setEmail(email);
        session.setIpHash(ipHasher.hash(ipResolver.resolve(request)));
        session.setCreatedAt(now);
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plus(properties.getTtl()));

        repository.save(session);
        cookieManager.write(response, session.getSessionId());

        return new SpringSessionLiteUser(userId, email, session.getSessionId());
    }

    @Transactional
    public Optional<SpringSessionLiteUser> validate(String sessionId, HttpServletRequest request) {
        return repository.findBySessionId(sessionId).flatMap(session -> {
            if (session.getExpiresAt().isBefore(Instant.now())) {
                return Optional.empty();
            }

            String currentIpHash = ipHasher.hash(ipResolver.resolve(request));

            if (!session.getIpHash().equals(currentIpHash)) {
                return Optional.empty();
            }

            session.setLastAccessedAt(Instant.now());
            return Optional.of(new SpringSessionLiteUser(session.getUserId(), session.getEmail(), session.getSessionId()));
        });
    }

    public void deleteExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }
}
