package com.arthexis.platform.app.admin;

import java.time.Instant;

public record ConnectorChangedEvent(
    String stationId,
    Integer evseId,
    Integer connectorId,
    String status,
    String connectorType,
    String availability,
    Instant occurredAt)
    implements AdminDomainEvent {}
