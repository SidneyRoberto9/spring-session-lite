package io.github.sidneyroberto9.spring_session_lite;

import io.github.sidneyroberto9.spring_session_lite.domain.SessionRepository;
import io.github.sidneyroberto9.spring_session_lite.sample.SampleApplication;
import io.github.sidneyroberto9.spring_session_lite.sample.SampleController;
import io.github.sidneyroberto9.spring_session_lite.security.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringSessionLiteApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void cleanDb() {
        sessionRepository.deleteAll();
    }

    private String base() {
        return "http://localhost:" + port;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void loginWritesCookieWithCorrectAttributes() {
        ResponseEntity<SessionUser> response = rest.postForEntity(
                base() + "/login",
                new SampleController.LoginRequest("1", "test@test.com"),
                SessionUser.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String setCookie = response.getHeaders().getFirst("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("M4SID=");
        assertThat(setCookie).containsIgnoringCase("HttpOnly");
        assertThat(setCookie).containsIgnoringCase("SameSite=Lax");
        assertThat(setCookie).containsIgnoringCase("Max-Age=28800");
    }

    @Test
    void meWithValidCookieReturns200() {
        String cookie = doLogin("user1", "user1@test.com");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookie);

        ResponseEntity<SessionUser> response = rest.exchange(
                base() + "/me", HttpMethod.GET, new HttpEntity<>(headers), SessionUser.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo("user1");
        assertThat(response.getBody().getEmail()).isEqualTo("user1@test.com");
    }

    @Test
    void meWithNoCookieReturns401() {
        ResponseEntity<String> response = rest.getForEntity(base() + "/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginEndpointReachableWithoutSession() {
        ResponseEntity<SessionUser> response = rest.postForEntity(
                base() + "/login",
                new SampleController.LoginRequest("x", "x@x.com"),
                SessionUser.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void meWithTamperedCookieReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "M4SID=tampered-session-id-that-does-not-exist");

        ResponseEntity<String> response = rest.exchange(
                base() + "/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meWithExpiredSessionReturns401() {
        String cookie = doLogin("user2", "user2@test.com");
        String sessionId = cookie.split("=")[1].split(";")[0];

        sessionRepository.findBySessionId(sessionId).ifPresent(s -> {
            s.setExpiresAt(Instant.now().minusSeconds(60));
            sessionRepository.save(s);
        });

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookie);

        ResponseEntity<String> response = rest.exchange(
                base() + "/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cleanupRemovesExpiredSessions() {
        String cookie = doLogin("user3", "user3@test.com");
        String sessionId = cookie.split("=")[1].split(";")[0];

        sessionRepository.findBySessionId(sessionId).ifPresent(s -> {
            s.setExpiresAt(Instant.now().minusSeconds(60));
            sessionRepository.save(s);
        });

        assertThat(sessionRepository.count()).isEqualTo(1);

        sessionRepository.deleteByExpiresAtBefore(Instant.now());

        assertThat(sessionRepository.count()).isEqualTo(0);
    }

    @Test
    void ipMismatchReturns401() {
        String cookie = doLogin("user4", "user4@test.com");
        String sessionId = cookie.split("=")[1].split(";")[0];

        sessionRepository.findBySessionId(sessionId).ifPresent(s -> {
            s.setIpHash("0000000000000000000000000000000000000000000000000000000000000000");
            sessionRepository.save(s);
        });

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookie);

        ResponseEntity<String> response = rest.exchange(
                base() + "/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String doLogin(String userId, String email) {
        ResponseEntity<SessionUser> response = rest.postForEntity(
                base() + "/login",
                new SampleController.LoginRequest(userId, email),
                SessionUser.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = response.getHeaders().getFirst("Set-Cookie");
        assertThat(setCookie).isNotNull();
        return setCookie.split(";")[0];
    }
}
