package com.arthexis.platform.security;

public record WebAuthnAssertionFinishRequest(
    String username, String challenge, String credentialId, long signCount) {}
