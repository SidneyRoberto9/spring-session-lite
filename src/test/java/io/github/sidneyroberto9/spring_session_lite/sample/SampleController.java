package io.github.sidneyroberto9.spring_session_lite.sample;

import io.github.sidneyroberto9.spring_session_lite.security.SessionUser;
import io.github.sidneyroberto9.spring_session_lite.service.SessionLiteService;
import io.github.sidneyroberto9.spring_session_lite.web.CurrentSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SampleController {

    private final SessionLiteService sessionService;

    @PostMapping("/login")
    public ResponseEntity<SessionUser> login(
            @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        SessionUser user = sessionService.login(body.getUserId(), body.getEmail(), request, response);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<SessionUser> me(@CurrentSession SessionUser user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String userId;
        private String email;
    }
}
