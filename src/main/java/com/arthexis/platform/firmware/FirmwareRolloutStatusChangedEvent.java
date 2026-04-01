package com.arthexis.platform.firmware;

import java.time.Instant;

public record FirmwareRolloutStatusChangedEvent(
    String campaignId,
    String stationId,
    String tenantId,
    String targetVersion,
    String rolloutState,
    Instant occurredAt) {}
