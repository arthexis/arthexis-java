package com.arthexis.platform.app.admin;

import java.time.Instant;

public sealed interface AdminDomainEvent
    permits ConnectorChangedEvent,
        OcppMessagePersistedEvent,
        StationStatusChangedEvent {

  String stationId();

  Instant occurredAt();
}
