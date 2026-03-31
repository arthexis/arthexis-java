package com.arthexis.platform.app.admin;

import java.time.Instant;

public sealed interface AdminDomainEvent
    permits ConnectorChangedEvent,
        OcppMessagePersistedEvent,
        StationStatusChangedEvent,
        TelemetrySummaryEvent {

  String stationId();

  Instant occurredAt();
}
