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
    for (OcppFtpServerProperties.Binding binding : properties.bindings()) {
      if (byBindingId.putIfAbsent(binding.id(), binding) != null) {
        throw new IllegalArgumentException("Duplicate FTP binding id: " + binding.id());
      }
      for (String chargerId : binding.chargerIds()) {
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

  public List<OcppFtpServerProperties.Binding> allBindings() {
    return List.copyOf(byBindingId.values());
  }

  public Optional<OcppFtpServerProperties.Binding> bindingForCharger(String chargerId) {
    return Optional.ofNullable(byChargerId.get(chargerId));
  }
}
