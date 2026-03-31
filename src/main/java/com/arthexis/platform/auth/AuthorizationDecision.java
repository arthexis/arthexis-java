package com.arthexis.platform.auth;

public record AuthorizationDecision(
    boolean accepted,
    String ocppStatus,
    String authMode,
    String accountExternalId,
    String loginUrl,
    String reason) {}
