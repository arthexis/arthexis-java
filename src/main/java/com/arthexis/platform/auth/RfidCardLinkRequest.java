package com.arthexis.platform.auth;

public record RfidCardLinkRequest(
    String cardUid,
    String accountExternalId,
    RfidAuthMode authMode,
    boolean requireApproval,
    String accountDisplayName,
    String email) {}
