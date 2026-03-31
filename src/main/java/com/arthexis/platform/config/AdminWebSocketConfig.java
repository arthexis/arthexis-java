package com.arthexis.platform.config;

import com.arthexis.platform.security.mfa.MfaSessionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class AdminWebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic/admin");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws/admin")
        .setAllowedOriginPatterns("*")
        .addInterceptors(new HttpSessionHandshakeInterceptor());
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, org.springframework.messaging.MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            if (accessor.getCommand() != StompCommand.SUBSCRIBE
                && accessor.getCommand() != StompCommand.SEND
                && accessor.getCommand() != StompCommand.CONNECT) {
              return message;
            }

            Authentication authentication = (Authentication) accessor.getUser();
            if (authentication == null || !authentication.isAuthenticated()) {
              throw new SecurityException("Authentication required for admin websocket channel");
            }

            boolean authorized =
                authentication.getAuthorities().stream()
                    .anyMatch(
                        grantedAuthority ->
                            "ROLE_ADMIN".equals(grantedAuthority.getAuthority())
                                || "ROLE_OPERATOR".equals(grantedAuthority.getAuthority()));
            if (!authorized) {
              throw new SecurityException("Admin or operator role required for admin websocket channel");
            }
            if (!Boolean.TRUE.equals(accessor.getSessionAttributes().get(MfaSessionService.MFA_VERIFIED))) {
              throw new SecurityException("Second factor verification required for admin websocket channel");
            }
            return message;
          }
        });
  }
}
