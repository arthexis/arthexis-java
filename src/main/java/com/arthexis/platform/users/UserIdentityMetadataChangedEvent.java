package com.arthexis.platform.users;

import java.time.Instant;

public record UserIdentityMetadataChangedEvent(
    String tenantId,
    String accountId,
    String operatorId,
    String customerId,
    String identityState,
    Instant occurredAt) {}
