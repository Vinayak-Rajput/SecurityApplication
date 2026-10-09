ALTER TABLE auth_sessions
    ADD COLUMN refresh_session_expires_at TIMESTAMP WITH TIME ZONE;

UPDATE auth_sessions
SET refresh_session_expires_at = refresh_token_expires_at;

ALTER TABLE auth_sessions
    ALTER COLUMN refresh_session_expires_at SET NOT NULL;
