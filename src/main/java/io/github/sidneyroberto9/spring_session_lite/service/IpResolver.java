package io.github.sidneyroberto9.spring_session_lite.service;

import io.github.sidneyroberto9.spring_session_lite.config.SpringSessionLiteProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IpResolver {

    private final SpringSessionLiteProperties properties;

    public String resolve(HttpServletRequest request) {
        if (properties.isTrustForwardedFor()) {

            String xff = request.getHeader("X-Forwarded-For");

            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
