package com.arthexis.platform.app.admin;

import java.time.Instant;

public record StationStatusChangedEvent(
    String stationId,
    String status,
    String previousStatus,
    String tenantId,
    String siteId,
    Instant occurredAt)
    implements AdminDomainEvent {}
