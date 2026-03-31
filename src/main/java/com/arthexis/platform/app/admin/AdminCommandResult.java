package com.arthexis.platform.app.admin;

import java.time.Instant;

public record AdminCommandResult(
    String commandId,
    String stationId,
    String component,
    String action,
    AdminCommandStatus status,
    String message,
    Instant occurredAt) {}
