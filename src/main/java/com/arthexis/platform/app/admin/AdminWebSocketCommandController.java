package com.arthexis.platform.app.admin;

import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class AdminWebSocketCommandController {

  public static final String ADMIN_COMMAND_DESTINATION = "/app/admin/commands";

  private final SimpMessagingTemplate messagingTemplate;
  private final AdminCommandGateway commandGateway;

  public AdminWebSocketCommandController(
      SimpMessagingTemplate messagingTemplate, AdminCommandGateway commandGateway) {
    this.messagingTemplate = messagingTemplate;
    this.commandGateway = commandGateway;
  }

  @MessageMapping("/admin/commands")
  public void relay(@Payload AdminCommandRequest command, Principal principal) {
    String user = principal == null ? "unknown" : principal.getName();
    AdminCommandResult result = commandGateway.submit(command, user);
    messagingTemplate.convertAndSend(
        AdminWebSocketDomainEventBridge.ADMIN_TOPIC_EVENTS,
        new AdminRealtimePayload(
            "admin.command.result",
            result.stationId(),
            result.occurredAt(),
            java.util.Map.of(
                "commandId", result.commandId(),
                "component", result.component(),
                "action", result.action(),
                "status", result.status(),
                "message", result.message())));
  }
}
