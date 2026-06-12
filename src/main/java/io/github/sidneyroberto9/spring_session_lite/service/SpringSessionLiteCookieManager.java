package io.github.sidneyroberto9.spring_session_lite.service;

import io.github.sidneyroberto9.spring_session_lite.config.SpringSessionLiteProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;

@RequiredArgsConstructor
public class SpringSessionLiteCookieManager {

    private final SpringSessionLiteProperties properties;

    public void write(HttpServletResponse response, String sessionId) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getCookieName(), sessionId)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path(properties.getCookiePath())
                .maxAge(properties.getTtl());

        if (properties.getCookieDomain() != null) {
            builder.domain(properties.getCookieDomain());
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path(properties.getCookiePath())
                .maxAge(0);

        if (properties.getCookieDomain() != null) {
            builder.domain(properties.getCookieDomain());
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }
}
