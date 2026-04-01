CREATE TABLE energy_account (
    id BIGSERIAL PRIMARY KEY,
    account_external_id VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(128),
    email VARCHAR(256),
    energy_tracking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rfid_card (
    id BIGSERIAL PRIMARY KEY,
    card_uid VARCHAR(128) NOT NULL UNIQUE,
    auth_mode VARCHAR(32) NOT NULL,
    energy_account_id BIGINT REFERENCES energy_account(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    link_approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    link_approved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE charge_point_login_session (
    id BIGSERIAL PRIMARY KEY,
    login_token VARCHAR(128) NOT NULL UNIQUE,
    station_id VARCHAR(64) NOT NULL,
    card_uid VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    account_external_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rfid_card_account ON rfid_card(energy_account_id);
CREATE INDEX idx_cp_login_station_card_status
    ON charge_point_login_session(station_id, card_uid, status, updated_at DESC);
