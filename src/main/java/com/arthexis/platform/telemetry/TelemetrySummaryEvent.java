package com.arthexis.platform.telemetry;

import java.time.Instant;

public record TelemetrySummaryEvent(
    String stationId,
    int totalSamples,
    int structuredSamples,
    int numericPayloadSamples,
    Instant sampledAt,
    Instant occurredAt) {}
