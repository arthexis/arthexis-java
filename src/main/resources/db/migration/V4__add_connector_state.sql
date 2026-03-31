CREATE TABLE charging_connector_state (
    id BIGSERIAL PRIMARY KEY,
    station_id VARCHAR(64) NOT NULL,
    evse_id INTEGER NOT NULL,
    connector_id INTEGER NOT NULL,
    connector_status VARCHAR(32) NOT NULL,
    connector_type VARCHAR(64),
    availability VARCHAR(32),
    last_status_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_connector_state_station_evse_connector
        UNIQUE (station_id, evse_id, connector_id)
);

CREATE INDEX idx_connector_state_station ON charging_connector_state(station_id);
CREATE INDEX idx_connector_state_station_status ON charging_connector_state(station_id, connector_status);
