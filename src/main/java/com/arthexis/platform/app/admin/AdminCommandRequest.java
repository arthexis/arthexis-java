package com.arthexis.platform.app.admin;

import java.util.Map;

public record AdminCommandRequest(
    String stationId, String component, String action, String chargerProfile, Map<String, Object> payload) {}
