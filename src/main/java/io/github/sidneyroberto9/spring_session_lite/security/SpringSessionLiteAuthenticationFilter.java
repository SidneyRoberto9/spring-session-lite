package io.github.sidneyroberto9.spring_session_lite.security;

import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteCookieManager;
import io.github.sidneyroberto9.spring_session_lite.service.SpringSessionLiteService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SpringSessionLiteAuthenticationFilter extends OncePerRequestFilter {

    private final SpringSessionLiteService sessionService;
    private final SpringSessionLiteCookieManager cookieManager;

    public SpringSessionLiteAuthenticationFilter(SpringSessionLiteService sessionService, SpringSessionLiteCookieManager cookieManager) {
        this.sessionService = sessionService;
        this.cookieManager = cookieManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String sessionId = cookieManager.read(request);

        if (sessionId == null) {
            chain.doFilter(request, response);
            return;
        }

        Optional<SpringSessionLiteUser> user = sessionService.validate(sessionId, request);

        if (user.isEmpty()) {
            // Invalid/expired/IP-mismatch cookie: do NOT short-circuit with 401 here — that would
            // also block permit-all paths (e.g. re-login). Drop the dead cookie, stay anonymous,
            // and let authorization + the AuthenticationEntryPoint decide the response.
            SecurityContextHolder.clearContext();
            cookieManager.clear(response);
            chain.doFilter(request, response);
            return;
        }

        authenticate(user.get());
        chain.doFilter(request, response);
    }

    private void authenticate(SpringSessionLiteUser user) {
        List<SimpleGrantedAuthority> authorities = user.roles().stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
