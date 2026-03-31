package com.arthexis.platform.charging;

import java.time.Instant;

public record ChargingStationAdminDetails(
    String displayName,
    String vendor,
    String model,
    String protocolVersion,
    String firmwareVersion,
    String tenantId,
    String siteId,
    Boolean enabled,
    Instant lastHeartbeatAt,
    Instant lastBootAt) {}
