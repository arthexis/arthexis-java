package com.arthexis.platform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  ApplicationModules modules = ApplicationModules.of(ArthexisApplication.class);

  @Test
  void verifiesModuleBoundaries() {
    modules.verify();
  }
}
