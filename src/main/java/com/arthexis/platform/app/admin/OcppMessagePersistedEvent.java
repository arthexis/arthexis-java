package com.arthexis.platform.app.admin;

import java.time.Instant;

public record OcppMessagePersistedEvent(
    String stationId,
    String sessionId,
    String direction,
    String action,
    String parseStatus,
    String resultStatus,
    Instant occurredAt)
    implements AdminDomainEvent {}
