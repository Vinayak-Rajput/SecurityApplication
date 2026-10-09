CREATE INDEX IF NOT EXISTS auth_sessions_username_revoked_idx
    ON auth_sessions (username, revoked);

CREATE INDEX IF NOT EXISTS auth_sessions_expiration_cleanup_idx
    ON auth_sessions (access_token_expires_at, refresh_token_expires_at)
    WHERE revoked = false;

CREATE INDEX IF NOT EXISTS auth_sessions_revoked_cleanup_idx
    ON auth_sessions (id)
    WHERE revoked = true;
