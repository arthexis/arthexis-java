package com.arthexis.platform.security;

public record WebAuthnRegistrationFinishRequest(
    String username,
    String challenge,
    String credentialId,
    String publicKeyCose,
    long signCount,
    String transports) {}
