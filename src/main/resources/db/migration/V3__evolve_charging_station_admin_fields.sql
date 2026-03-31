ALTER TABLE charging_station
    ADD COLUMN display_name VARCHAR(128),
    ADD COLUMN vendor VARCHAR(128),
    ADD COLUMN model VARCHAR(128),
    ADD COLUMN protocol_version VARCHAR(32),
    ADD COLUMN firmware_version VARCHAR(64),
    ADD COLUMN tenant_id VARCHAR(64),
    ADD COLUMN site_id VARCHAR(64),
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_boot_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE charging_station
SET
    last_heartbeat_at = COALESCE(last_heartbeat_at, last_seen_at),
    created_at = COALESCE(created_at, last_seen_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, last_seen_at, CURRENT_TIMESTAMP),
    enabled = COALESCE(enabled, TRUE);
