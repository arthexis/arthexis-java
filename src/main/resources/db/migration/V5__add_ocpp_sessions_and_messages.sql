CREATE TABLE ocpp_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(128) NOT NULL UNIQUE,
    station_id VARCHAR(64),
    connected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    disconnected_at TIMESTAMP WITH TIME ZONE,
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ocpp_message_record (
    id BIGSERIAL PRIMARY KEY,
    session_record_id BIGINT REFERENCES ocpp_session(id),
    session_id VARCHAR(128) NOT NULL,
    station_id VARCHAR(64),
    direction VARCHAR(16) NOT NULL,
    message_type VARCHAR(32),
    action VARCHAR(64),
    message_id VARCHAR(128),
    payload_snapshot VARCHAR(8000) NOT NULL,
    payload_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    parse_status VARCHAR(32) NOT NULL,
    result_status VARCHAR(64),
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ocpp_session_station_last_message
    ON ocpp_session(station_id, last_message_at DESC);

CREATE INDEX idx_ocpp_msg_station_action_sampled
    ON ocpp_message_record(station_id, action, sampled_at DESC);

CREATE INDEX idx_ocpp_msg_station_sampled
    ON ocpp_message_record(station_id, sampled_at DESC);
