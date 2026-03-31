package com.arthexis.platform.auth;

public interface RfidAuthorizationGateway {

  AuthorizationDecision authorize(String stationId, String cardUid);
}
