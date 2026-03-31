package com.arthexis.platform.ocpp;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arthexis.ocpp.commands")
public class OcppCommandDispatchProperties {

  private Duration retryDelay = Duration.ofSeconds(20);
  private Duration ackTimeout = Duration.ofSeconds(45);
  private int maxRetries = 3;
  private Map<String, Set<String>> profileCapabilities = defaultProfileCapabilities();

  public Duration getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(Duration retryDelay) {
    this.retryDelay = retryDelay;
  }

  public Duration getAckTimeout() {
    return ackTimeout;
  }

  public void setAckTimeout(Duration ackTimeout) {
    this.ackTimeout = ackTimeout;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Map<String, Set<String>> getProfileCapabilities() {
    return profileCapabilities;
  }

  public void setProfileCapabilities(Map<String, Set<String>> profileCapabilities) {
    this.profileCapabilities = normalize(profileCapabilities);
  }

  private Map<String, Set<String>> defaultProfileCapabilities() {
    Map<String, Set<String>> defaults = new LinkedHashMap<>();
    defaults.put(
        "python-ocpp16",
        orderedSet(
            "RemoteStartTransaction",
            "RemoteStopTransaction",
            "ChangeAvailability",
            "Reset",
            "GetDiagnostics",
            "UpdateFirmware"));
    defaults.put(
        "python-ocpp2x",
        orderedSet(
            "RequestStartTransaction",
            "RequestStopTransaction",
            "SetChargingProfile",
            "Reset",
            "ChangeAvailability",
            "UpdateFirmware"));
    return defaults;
  }

  private Map<String, Set<String>> normalize(Map<String, Set<String>> source) {
    Map<String, Set<String>> normalized = new LinkedHashMap<>(defaultProfileCapabilities());
    if (source == null || source.isEmpty()) {
      return normalized;
    }

    source.forEach(
        (profile, actions) -> {
          if (profile != null && !profile.isBlank()) {
            normalized.put(profile.trim().toLowerCase(), orderedSet(actions));
          }
        });
    return normalized;
  }

  private Set<String> orderedSet(String... values) {
    Set<String> set = new LinkedHashSet<>();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        set.add(value.trim());
      }
    }
    return set;
  }

  private Set<String> orderedSet(Set<String> values) {
    if (values == null || values.isEmpty()) {
      return Set.of();
    }
    Set<String> set = new LinkedHashSet<>();
    values.forEach(
        value -> {
          if (value != null && !value.isBlank()) {
            set.add(value.trim());
          }
        });
    return set;
  }
}
