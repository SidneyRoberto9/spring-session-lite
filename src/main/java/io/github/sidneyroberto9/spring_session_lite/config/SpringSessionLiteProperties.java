package io.github.sidneyroberto9.spring_session_lite.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring-session-lite")
public class SpringSessionLiteProperties {

    private String cookieName = "M4SID";

    private Duration ttl = Duration.ofHours(8);

    private boolean cookieSecure = true;

    private String cookieSameSite = "Lax";

    private String cookiePath = "/";

    private String cookieDomain;

    private int sessionIdLength = 16;

    /**
     * Salt used in HMAC-SHA256 IP hashing. Override in production with a strong secret.
     */
    private String ipHashSalt = "spring-session-lite";

    /**
     * Trust X-Forwarded-For header for client IP resolution. Enable only behind a trusted proxy.
     */
    private boolean trustForwardedFor = false;

    private String cleanupCron = "0 */30 * * * *";

    private boolean enabled = true;

    private List<String> permitAllPaths = new ArrayList<>(List.of("/login", "/auth/**", "/public/**"));
}
