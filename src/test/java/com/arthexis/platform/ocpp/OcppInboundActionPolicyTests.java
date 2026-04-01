package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OcppInboundActionPolicyTests {

  @Test
  void classifiesSupportedAndIgnoredActionsByProfile() {
    assertThat(OcppInboundActionPolicy.supportLevel("python-ocpp16", "StartTransaction"))
        .isEqualTo(OcppInboundActionPolicy.ActionSupportLevel.SUPPORTED);
    assertThat(OcppInboundActionPolicy.supportLevel("python-ocpp2x", "NotifyEvent"))
        .isEqualTo(OcppInboundActionPolicy.ActionSupportLevel.SUPPORTED);
    assertThat(OcppInboundActionPolicy.supportLevel("python-ocpp16", "NotifyEvent"))
        .isEqualTo(OcppInboundActionPolicy.ActionSupportLevel.IGNORED);
  }

  @Test
  void classifiesUnknownActionsAsUnsupportedButAccepted() {
    assertThat(OcppInboundActionPolicy.supportLevel("python-ocpp16", "GetConfiguration"))
        .isEqualTo(OcppInboundActionPolicy.ActionSupportLevel.UNSUPPORTED_BUT_ACCEPTED);
  }
}
