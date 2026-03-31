CREATE TABLE ocpp_command (
    id BIGSERIAL PRIMARY KEY,
    command_id VARCHAR(64) NOT NULL UNIQUE,
    station_id VARCHAR(64) NOT NULL,
    component VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payload_json TEXT NOT NULL,
    message_id VARCHAR(128),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    requested_by VARCHAR(128),
    failure_reason VARCHAR(512),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    queued_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ocpp_command_station_component_action
    ON ocpp_command(station_id, component, action, updated_at DESC);

CREATE INDEX idx_ocpp_command_status_next_attempt
    ON ocpp_command(status, next_attempt_at);

CREATE UNIQUE INDEX idx_ocpp_command_message_id
    ON ocpp_command(message_id);
