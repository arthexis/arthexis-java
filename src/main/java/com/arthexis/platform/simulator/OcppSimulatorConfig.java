package com.arthexis.platform.simulator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
@EnableConfigurationProperties(OcppSimulatorProperties.class)
public class OcppSimulatorConfig {

  @Bean
  @ConditionalOnProperty(prefix = "arthexis.ocpp.simulator", name = "enabled", havingValue = "true")
  WebSocketClient ocppSimulatorWebSocketClient() {
    return new StandardWebSocketClient();
  }
}
