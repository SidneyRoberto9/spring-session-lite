# Spring Session Lite

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Lightweight **database-backed session authentication** starter for Spring Boot 3 — a JWT
alternative that keeps session state in your existing database (no Redis). The client carries a
short opaque NanoID in an **HttpOnly** cookie; the real state lives in a table created from your
own `spring.datasource.*`.

- Zero external infra — reuses your DataSource.
- Auto-configured: cookie, filter, security context, `@SpringSessionLiteCurrentSession` injection and expired-session cleanup all wired automatically.
- One call on login; everything else is automatic.

## Install

```xml
<dependency>
    <groupId>io.github.sidneyroberto9</groupId>
    <artifactId>spring-session-lite</artifactId>
    <version>2.0.0</version>
</dependency>
```

Add your JDBC driver (`mysql-connector-j`, `postgresql`, …). The library brings
`spring-boot-starter-data-jpa`, `-security` and `-web` transitively.

> **Upgrading from 1.0.x?** 2.0.0 has breaking changes (renamed table/cookie, `record` principal).
> See [`MIGRATION.md`](MIGRATION.md).

## Quickstart

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SpringSessionLiteService sessionService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<SpringSessionLiteUser> login(
            @RequestBody LoginRequest body,
            HttpServletRequest request, HttpServletResponse response) {

        User user = userService.authenticate(body.email(), body.password()); // your logic
        var session = sessionService.login(
                String.valueOf(user.getId()), user.getEmail(),
                List.of("ADMIN"),            // optional roles → ROLE_ADMIN authorities
                request, response);          // writes the SLSID cookie
        return ResponseEntity.ok(session);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.logout(request, response);   // deletes the session + clears the cookie
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public SpringSessionLiteUser me(@SpringSessionLiteCurrentSession SpringSessionLiteUser user) {
        return user;
    }
}
```

Outside controllers, inject `SpringSessionLiteUserService` and call `currentUser()`.

## Key properties (prefix `spring-session-lite`)

| Property | Default | Notes |
|----------|---------|-------|
| `cookie-name` | `SLSID` | Session cookie name. |
| `ttl` | `8h` | Session lifetime / cookie Max-Age. |
| `cookie-secure` | `true` | HTTPS-only cookie. |
| `cookie-same-site` | `Lax` | `Lax` / `Strict` / `None`. |
| `ip-hash-salt` | _(default)_ | **Override in production.** Warns at startup if left default. |
| `trust-forwarded-for` / `trusted-proxy-count` | `false` / `1` | XFF handling — picks the client IP before the trusted-proxy chain (never the spoofable left-most). |
| `csrf-enabled` | `false` | Enable cookie-based CSRF tokens. |
| `cors-enabled` / `cors-allowed-origins` | `false` / _(empty)_ | Cross-origin cookie auth. |
| `update-last-accessed` / `last-accessed-throttle` | `true` / `5m` | Throttled last-accessed writes (avoids a DB write per request). |
| `sliding-expiration` | `false` | Slide expiry forward on activity. |
| `cleanup-enabled` / `cleanup-cron` | `true` / every 30 min | Expired-session cleanup. |
| `permit-all-paths` | `/login, /auth/**, /public/**` | Open paths on the default chain. |

Full reference: [`docs/02-configuracao-application-properties.md`](docs/02-configuracao-application-properties.md).

## How it works

`POST /login` persists a session row and sets the `SLSID` cookie. A filter inside the Spring
Security chain reads the cookie on each request, validates it (existence, expiration, IP hash),
and populates the `SecurityContext`. Expired rows are pruned by a scheduled task.

Details: [`docs/03-como-funciona.md`](docs/03-como-funciona.md) ·
Install guide: [`docs/01-instalacao-e-uso.md`](docs/01-instalacao-e-uso.md).

## Schema

With `ddl-auto=update` the table `spring_session_lite_sessions` is created automatically. For
`validate`/`none`, apply [`src/main/resources/db/spring-session-lite-schema.sql`](src/main/resources/db/spring-session-lite-schema.sql).

## License

MIT © Sidney Roberto
