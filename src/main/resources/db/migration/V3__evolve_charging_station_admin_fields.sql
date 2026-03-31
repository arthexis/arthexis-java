ALTER TABLE charging_station ADD COLUMN display_name VARCHAR(128);
ALTER TABLE charging_station ADD COLUMN vendor VARCHAR(128);
ALTER TABLE charging_station ADD COLUMN model VARCHAR(128);
ALTER TABLE charging_station ADD COLUMN protocol_version VARCHAR(32);
ALTER TABLE charging_station ADD COLUMN firmware_version VARCHAR(64);
ALTER TABLE charging_station ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE charging_station ADD COLUMN site_id VARCHAR(64);
ALTER TABLE charging_station ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE charging_station ADD COLUMN last_heartbeat_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE charging_station ADD COLUMN last_boot_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE charging_station ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE charging_station ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE charging_station
SET
    last_heartbeat_at = COALESCE(last_heartbeat_at, last_seen_at),
    created_at = COALESCE(created_at, last_seen_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, last_seen_at, CURRENT_TIMESTAMP),
    enabled = COALESCE(enabled, TRUE);
