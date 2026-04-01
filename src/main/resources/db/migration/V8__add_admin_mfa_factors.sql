create table if not exists admin_webauthn_credentials (
    id bigserial primary key,
    username varchar(255) not null,
    credential_id varchar(255) not null unique,
    public_key_cose text not null,
    sign_count bigint not null default 0,
    transports varchar(256),
    created_at timestamp with time zone not null,
    last_used_at timestamp with time zone
);

create index if not exists idx_admin_webauthn_credentials_username
    on admin_webauthn_credentials (username);

create table if not exists admin_totp_factors (
    id bigserial primary key,
    username varchar(255) not null unique,
    secret varchar(512) not null,
    enabled boolean not null default false,
    enrolled_at timestamp with time zone not null,
    verified_at timestamp with time zone
);

create table if not exists admin_step_up_sessions (
    token varchar(128) primary key,
    username varchar(255) not null,
    factor_type varchar(32) not null,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null
);

create index if not exists idx_admin_step_up_sessions_username_expires_at
    on admin_step_up_sessions (username, expires_at);
