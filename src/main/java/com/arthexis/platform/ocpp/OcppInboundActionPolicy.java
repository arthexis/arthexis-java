package com.arthexis.platform.ocpp;

import java.util.Map;
import java.util.Set;

public final class OcppInboundActionPolicy {

  public static final String PROFILE_PYTHON_OCPP16 = "python-ocpp16";
  public static final String PROFILE_PYTHON_OCPP2X = "python-ocpp2x";

  private static final Map<String, Set<String>> SUPPORTED_ACTIONS =
      Map.of(
          PROFILE_PYTHON_OCPP16,
          Set.of(
              "BootNotification",
              "Heartbeat",
              "StatusNotification",
              "MeterValues",
              "Authorize",
              "StartTransaction",
              "StopTransaction",
              "DiagnosticsStatusNotification",
              "FirmwareStatusNotification"),
          PROFILE_PYTHON_OCPP2X,
          Set.of(
              "BootNotification",
              "Heartbeat",
              "StatusNotification",
              "MeterValues",
              "Authorize",
              "TransactionEvent",
              "SecurityEventNotification",
              "NotifyEvent",
              "AvailabilityStatusNotification"));

  private static final Map<String, Set<String>> PARTIALLY_SUPPORTED_ACTIONS =
      Map.of(
          PROFILE_PYTHON_OCPP16,
          Set.of("TransactionEvent", "AvailabilityStatusNotification"),
          PROFILE_PYTHON_OCPP2X,
          Set.of(
              "StartTransaction",
              "StopTransaction",
              "DiagnosticsStatusNotification",
              "FirmwareStatusNotification"));

  private static final Map<String, Set<String>> IGNORED_ACTIONS =
      Map.of(
          PROFILE_PYTHON_OCPP16,
          Set.of("DataTransfer", "SecurityEventNotification", "NotifyEvent"),
          PROFILE_PYTHON_OCPP2X,
          Set.of("SignCertificate", "Get15118EVCertificate", "DataTransfer"));

  private OcppInboundActionPolicy() {}

  public static ActionSupportLevel supportLevel(String profile, String action) {
    String normalizedProfile = normalizeProfile(profile);
    if (SUPPORTED_ACTIONS.getOrDefault(normalizedProfile, Set.of()).contains(action)) {
      return ActionSupportLevel.SUPPORTED;
    }
    if (PARTIALLY_SUPPORTED_ACTIONS.getOrDefault(normalizedProfile, Set.of()).contains(action)) {
      return ActionSupportLevel.PARTIALLY_SUPPORTED;
    }
    if (IGNORED_ACTIONS.getOrDefault(normalizedProfile, Set.of()).contains(action)) {
      return ActionSupportLevel.IGNORED;
    }
    return ActionSupportLevel.UNSUPPORTED_BUT_ACCEPTED;
  }

  public static String normalizeProfile(String profile) {
    if (profile == null || profile.isBlank()) {
      return PROFILE_PYTHON_OCPP16;
    }
    String lower = profile.toLowerCase();
    if (lower.contains("2")) {
      return PROFILE_PYTHON_OCPP2X;
    }
    if (lower.contains("16")) {
      return PROFILE_PYTHON_OCPP16;
    }
    return lower;
  }

  public enum ActionSupportLevel {
    SUPPORTED,
    PARTIALLY_SUPPORTED,
    IGNORED,
    UNSUPPORTED_BUT_ACCEPTED
  }
}
