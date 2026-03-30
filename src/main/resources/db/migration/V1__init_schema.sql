CREATE TABLE charging_station (
    id BIGSERIAL PRIMARY KEY,
    station_id VARCHAR(64) UNIQUE NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE
);
