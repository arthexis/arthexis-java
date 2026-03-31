package com.arthexis.platform.app.admin;

import java.time.Instant;
import java.util.Map;

public record AdminRealtimePayload(
    String eventType, String stationId, Instant occurredAt, Map<String, Object> details) {}
