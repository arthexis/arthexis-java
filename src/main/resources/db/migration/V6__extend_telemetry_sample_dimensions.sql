ALTER TABLE telemetry_sample ADD COLUMN IF NOT EXISTS scope_type VARCHAR(32);
ALTER TABLE telemetry_sample ADD COLUMN IF NOT EXISTS scope_identifier VARCHAR(128);
ALTER TABLE telemetry_sample ADD COLUMN IF NOT EXISTS unit VARCHAR(32);
ALTER TABLE telemetry_sample ADD COLUMN IF NOT EXISTS phase VARCHAR(32);
ALTER TABLE telemetry_sample ADD COLUMN IF NOT EXISTS location VARCHAR(32);
ALTER TABLE telemetry_sample ADD COLUMN IF NOT EXISTS context VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_telemetry_station_sampled_at
    ON telemetry_sample (station_id, sampled_at DESC);

CREATE INDEX IF NOT EXISTS idx_telemetry_connector_sampled_at
    ON telemetry_sample (scope_type, scope_identifier, sampled_at DESC);

CREATE INDEX IF NOT EXISTS idx_telemetry_measurand_sampled_at
    ON telemetry_sample (metric_name, sampled_at DESC);
