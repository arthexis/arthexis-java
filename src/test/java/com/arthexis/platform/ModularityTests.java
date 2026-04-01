package com.arthexis.platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  ApplicationModules modules = ApplicationModules.of(ArthexisApplication.class);

  @Test
  void verifiesModuleBoundaries() {
    modules.verify();
  }

  @Test
  void includesUsersBillingAndFirmwareModules() {
    assertThat(modules.getModuleByName("users")).isPresent();
    assertThat(modules.getModuleByName("billing")).isPresent();
    assertThat(modules.getModuleByName("firmware")).isPresent();
  }
}
