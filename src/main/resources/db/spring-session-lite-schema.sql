-- Spring Session Lite — DDL for apps using ddl-auto=none/validate
-- Run once in your database before starting the application.
-- MySQL / MariaDB
CREATE TABLE m4_sessions (
    id              VARCHAR(36)  NOT NULL,
    session_id      VARCHAR(32)  NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    ip_hash         VARCHAR(64)  NOT NULL,
    created_at      DATETIME(6),
    expires_at      DATETIME(6)  NOT NULL,
    last_accessed_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY idx_m4_sessions_session_id (session_id),
    KEY idx_m4_sessions_expires_at (expires_at)
);

-- PostgreSQL variant:
-- CREATE TABLE m4_sessions (
--     id               VARCHAR(36)  NOT NULL PRIMARY KEY,
--     session_id       VARCHAR(32)  NOT NULL UNIQUE,
--     user_id          VARCHAR(255) NOT NULL,
--     email            VARCHAR(255),
--     ip_hash          VARCHAR(64)  NOT NULL,
--     created_at       TIMESTAMP,
--     expires_at       TIMESTAMP    NOT NULL,
--     last_accessed_at TIMESTAMP
-- );
-- CREATE INDEX idx_m4_sessions_expires_at ON m4_sessions (expires_at);

-- H2 (tests):
-- Same as PostgreSQL variant above.
