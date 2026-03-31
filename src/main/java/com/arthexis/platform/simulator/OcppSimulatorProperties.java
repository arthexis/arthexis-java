package com.arthexis.platform.simulator;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arthexis.ocpp.simulator")
public class OcppSimulatorProperties {

  private boolean enabled = false;
  private String chargePointId = "sim-cp-001";
  private String csmsUrl = "ws://localhost:8080/ws/ocpp";
  private Duration heartbeatInterval = Duration.ofSeconds(60);
  private Duration reconnectDelay = Duration.ofSeconds(10);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getChargePointId() {
    return chargePointId;
  }

  public void setChargePointId(String chargePointId) {
    this.chargePointId = chargePointId;
  }

  public String getCsmsUrl() {
    return csmsUrl;
  }

  public void setCsmsUrl(String csmsUrl) {
    this.csmsUrl = csmsUrl;
  }

  public Duration getHeartbeatInterval() {
    return heartbeatInterval;
  }

  public void setHeartbeatInterval(Duration heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
  }

  public Duration getReconnectDelay() {
    return reconnectDelay;
  }

  public void setReconnectDelay(Duration reconnectDelay) {
    this.reconnectDelay = reconnectDelay;
  }
}
