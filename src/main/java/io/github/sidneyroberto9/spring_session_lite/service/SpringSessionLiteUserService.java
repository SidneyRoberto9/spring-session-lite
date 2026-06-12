package io.github.sidneyroberto9.spring_session_lite.service;

import io.github.sidneyroberto9.spring_session_lite.security.SpringSessionLiteUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SpringSessionLiteUserService {

    public Optional<SpringSessionLiteUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SpringSessionLiteUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
