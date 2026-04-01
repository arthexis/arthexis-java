package com.arthexis.platform.billing;

import java.math.BigDecimal;
import java.time.Instant;

public record BillingSessionRatedEvent(
    String sessionId,
    String stationId,
    String tenantId,
    String accountId,
    long ratedWh,
    BigDecimal ratedAmount,
    String currency,
    boolean invoiceReady,
    Instant occurredAt) {}
