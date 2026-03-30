package com.arthexis.platform.ocpp;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class OcppWebSocketConfig implements WebSocketConfigurer {

  private final OcppWebSocketHandler ocppWebSocketHandler;

  public OcppWebSocketConfig(OcppWebSocketHandler ocppWebSocketHandler) {
    this.ocppWebSocketHandler = ocppWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(ocppWebSocketHandler, "/ws/ocpp").setAllowedOriginPatterns("*");
  }
}
