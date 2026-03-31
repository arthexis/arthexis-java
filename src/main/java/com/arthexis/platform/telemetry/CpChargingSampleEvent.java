package com.arthexis.platform.telemetry;

import java.time.Instant;

public record CpChargingSampleEvent(
    String stationId, String metricName, double metricValue, String unit, Instant sampledAt) {}
