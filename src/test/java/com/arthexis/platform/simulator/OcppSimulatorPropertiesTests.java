package com.arthexis.platform.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OcppSimulatorPropertiesTests {

  @Test
  void hasDefaultCsmEndpointPointingAtLocalOcppWebsocket() {
    OcppSimulatorProperties properties = new OcppSimulatorProperties();

    assertThat(properties.isEnabled()).isFalse();
    assertThat(properties.getCsmsUrl()).isEqualTo("ws://localhost:8080/ws/ocpp");
    assertThat(properties.getChargePointId()).isEqualTo("sim-cp-001");
  }
}
