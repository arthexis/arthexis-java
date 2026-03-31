package com.arthexis.platform.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "arthexis.rfid")
public class RfidAuthProperties {

  private String loginBaseUrl = "https://app.arthexis.local/energy/login";
  private Duration loginSessionTtl = Duration.ofMinutes(10);
  private Duration trustedLoginWindow = Duration.ofMinutes(15);

  public String getLoginBaseUrl() {
    return loginBaseUrl;
  }

  public void setLoginBaseUrl(String loginBaseUrl) {
    this.loginBaseUrl = loginBaseUrl;
  }

  public Duration getLoginSessionTtl() {
    return loginSessionTtl;
  }

  public void setLoginSessionTtl(Duration loginSessionTtl) {
    this.loginSessionTtl = loginSessionTtl;
  }

  public Duration getTrustedLoginWindow() {
    return trustedLoginWindow;
  }

  public void setTrustedLoginWindow(Duration trustedLoginWindow) {
    this.trustedLoginWindow = trustedLoginWindow;
  }
}
