package com.arthexis.platform.ocpp;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arthexis.ocpp.commands")
public class OcppCommandDispatchProperties {

  private Duration retryDelay = Duration.ofSeconds(20);
  private Duration ackTimeout = Duration.ofSeconds(45);
  private int maxRetries = 3;

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
}
