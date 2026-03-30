package com.arthexis.platform.ocpp;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OcppMessage(String messageType, String messageId, String action, Object payload) {}
