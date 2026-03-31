package com.arthexis.platform.app.admin;

import java.time.Instant;

public record OcppCommandStatusChangedEvent(
    String commandId,
    String stationId,
    String component,
    String action,
    AdminCommandStatus status,
    String messageId,
    String detail,
    Instant occurredAt) {}
