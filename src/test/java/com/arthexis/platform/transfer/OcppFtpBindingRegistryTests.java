package com.arthexis.platform.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class OcppFtpBindingRegistryTests {

  @Test
  void resolvesBindingForConfiguredCharger() {
    OcppFtpServerProperties properties = new OcppFtpServerProperties();
    OcppFtpServerProperties.Binding binding = new OcppFtpServerProperties.Binding();
    binding.setId("fleet-alpha");
    binding.setUsername("alpha");
    binding.setPassword("secret");
    binding.setChargerIds(List.of("cp-001", "cp-002"));
    properties.setBindings(List.of(binding));

    OcppFtpBindingRegistry registry = new OcppFtpBindingRegistry(properties);
    registry.initialize();

    assertThat(registry.bindingForCharger("cp-001")).isPresent();
    assertThat(registry.bindingForCharger("cp-001").orElseThrow().id()).isEqualTo("fleet-alpha");
    assertThat(registry.bindingForCharger("unknown")).isEmpty();
  }

  @Test
  void rejectsDuplicateChargerAssignments() {
    OcppFtpServerProperties properties = new OcppFtpServerProperties();

    OcppFtpServerProperties.Binding alpha = new OcppFtpServerProperties.Binding();
    alpha.setId("alpha");
    alpha.setUsername("alpha");
    alpha.setPassword("s1");
    alpha.setChargerIds(List.of("cp-001"));

    OcppFtpServerProperties.Binding beta = new OcppFtpServerProperties.Binding();
    beta.setId("beta");
    beta.setUsername("beta");
    beta.setPassword("s2");
    beta.setChargerIds(List.of("cp-001"));

    properties.setBindings(List.of(alpha, beta));

    OcppFtpBindingRegistry registry = new OcppFtpBindingRegistry(properties);
    assertThatThrownBy(registry::initialize).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsDuplicateUsernames() {
    OcppFtpServerProperties properties = new OcppFtpServerProperties();

    OcppFtpServerProperties.Binding alpha = new OcppFtpServerProperties.Binding();
    alpha.setId("alpha");
    alpha.setUsername("shared");
    alpha.setPassword("s1");
    alpha.setChargerIds(List.of("cp-001"));

    OcppFtpServerProperties.Binding beta = new OcppFtpServerProperties.Binding();
    beta.setId("beta");
    beta.setUsername("shared");
    beta.setPassword("s2");
    beta.setChargerIds(List.of("cp-002"));

    properties.setBindings(List.of(alpha, beta));

    OcppFtpBindingRegistry registry = new OcppFtpBindingRegistry(properties);
    assertThatThrownBy(registry::initialize).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsPathTraversalInBindingIdAndChargerId() {
    OcppFtpServerProperties properties = new OcppFtpServerProperties();

    OcppFtpServerProperties.Binding binding = new OcppFtpServerProperties.Binding();
    binding.setId("../fleet");
    binding.setUsername("alpha");
    binding.setPassword("secret");
    binding.setChargerIds(List.of("cp-001"));
    properties.setBindings(List.of(binding));

    OcppFtpBindingRegistry registry = new OcppFtpBindingRegistry(properties);
    assertThatThrownBy(registry::initialize).isInstanceOf(IllegalArgumentException.class);
  }
}
