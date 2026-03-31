package com.arthexis.platform.app.admin;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class AdminWebSocketCommandController {

  public static final String ADMIN_COMMAND_DESTINATION = "/app/admin/commands";

  private final SimpMessagingTemplate messagingTemplate;

  public AdminWebSocketCommandController(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @MessageMapping("/admin/commands")
  public void relay(@Payload Map<String, Object> command, Principal principal) {
    String user = principal == null ? "unknown" : principal.getName();
    AdminRealtimePayload payload =
        new AdminRealtimePayload(
            "admin.command.received",
            String.valueOf(command.getOrDefault("stationId", "unknown")),
            Instant.now(),
            Map.of("user", user, "command", command));
    messagingTemplate.convertAndSend(AdminWebSocketDomainEventBridge.ADMIN_TOPIC_EVENTS, payload);
  }
}
