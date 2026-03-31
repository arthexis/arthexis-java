CREATE TABLE admin_webauthn_credential (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(128) NOT NULL,
    credential_id VARCHAR(255) NOT NULL UNIQUE,
    public_key_cose TEXT NOT NULL,
    sign_count BIGINT NOT NULL DEFAULT 0,
    transports VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_admin_webauthn_username ON admin_webauthn_credential (username);

CREATE TABLE admin_totp_enrollment (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(128) NOT NULL UNIQUE,
    secret VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE admin_mfa_challenge (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(128) NOT NULL,
    factor_type VARCHAR(64) NOT NULL,
    challenge VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_admin_mfa_challenge UNIQUE (username, factor_type, challenge)
);

CREATE INDEX idx_admin_mfa_challenge_lookup
    ON admin_mfa_challenge (username, factor_type, challenge);
