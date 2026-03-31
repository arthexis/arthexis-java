package com.arthexis.platform.ocpp;

import java.util.Map;

public record OcppBridgeResponse(String stationId, Map<String, Object> payload, String resultStatus) {}
