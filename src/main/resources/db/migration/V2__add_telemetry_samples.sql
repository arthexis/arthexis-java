CREATE TABLE telemetry_sample (
    id BIGSERIAL PRIMARY KEY,
    station_id VARCHAR(64) NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_telemetry_station_sampled_at
    ON telemetry_sample (station_id, sampled_at DESC);
