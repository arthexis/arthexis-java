package com.arthexis.platform.transfer;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OcppFtpBindingRegistry {

  private final OcppFtpServerProperties properties;
  private final Map<String, OcppFtpServerProperties.Binding> byBindingId = new HashMap<>();
  private final Map<String, OcppFtpServerProperties.Binding> byChargerId = new HashMap<>();

  public OcppFtpBindingRegistry(OcppFtpServerProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void initialize() {
    byBindingId.clear();
    byChargerId.clear();
    Map<String, OcppFtpServerProperties.Binding> byUsername = new HashMap<>();
    for (OcppFtpServerProperties.Binding binding : properties.bindings()) {
      validateSegment(binding.id(), "binding id");
      if (byUsername.putIfAbsent(binding.username(), binding) != null) {
        throw new IllegalArgumentException("Duplicate FTP binding username: " + binding.username());
      }
      if (byBindingId.putIfAbsent(binding.id(), binding) != null) {
        throw new IllegalArgumentException("Duplicate FTP binding id: " + binding.id());
      }
      for (String chargerId : binding.chargerIds()) {
        validateSegment(chargerId, "charger id");
        OcppFtpServerProperties.Binding existing = byChargerId.putIfAbsent(chargerId, binding);
        if (existing != null) {
          throw new IllegalArgumentException(
              "Charger '" + chargerId + "' mapped to multiple FTP bindings: "
                  + existing.id()
                  + ", "
                  + binding.id());
        }
      }
    }
  }

  private void validateSegment(String value, String fieldName) {
    if (value == null || value.isBlank() || value.contains("..")) {
      throw new IllegalArgumentException("Invalid FTP " + fieldName + ": " + value);
    }
  }

  public List<OcppFtpServerProperties.Binding> allBindings() {
    return List.copyOf(byBindingId.values());
  }

  public Optional<OcppFtpServerProperties.Binding> bindingForCharger(String chargerId) {
    return Optional.ofNullable(byChargerId.get(chargerId));
  }
}
