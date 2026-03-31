package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;

import com.arthexis.platform.app.admin.AdminCommandGateway;
import com.arthexis.platform.app.admin.AdminCommandRequest;
import com.arthexis.platform.app.admin.AdminCommandResult;
import com.arthexis.platform.app.admin.AdminCommandStatus;
import com.arthexis.platform.app.admin.AdminRealtimePayload;
import com.arthexis.platform.app.admin.AdminWebSocketCommandController;
import com.arthexis.platform.app.admin.AdminWebSocketDomainEventBridge;
import com.arthexis.platform.app.admin.ConnectorChangedEvent;
import com.arthexis.platform.app.admin.OcppMessagePersistedEvent;
import com.arthexis.platform.app.admin.StationStatusChangedEvent;
import com.arthexis.platform.app.admin.TelemetrySummaryEvent;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class AdminWebSocketStreamsAndCommandsTests {

  @Test
  void streamsDomainEventsToAdminTopic() {
    CapturingMessageChannel channel = new CapturingMessageChannel();
    SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(channel);
    AdminWebSocketDomainEventBridge bridge = new AdminWebSocketDomainEventBridge(messagingTemplate);

    bridge.onStationStatusChanged(
        new StationStatusChangedEvent(
            "CP-ADMIN-1", "CHARGING", "AVAILABLE", "tenant-a", "site-a", Instant.now()));
    bridge.onConnectorChanged(
        new ConnectorChangedEvent(
            "CP-ADMIN-1", 1, 2, "CHARGING", "CCS2", "OPERATIVE", Instant.now()));
    bridge.onTelemetrySummary(
        new TelemetrySummaryEvent("CP-ADMIN-1", 8, 3, 5, Instant.now(), Instant.now()));
    bridge.onSessionEvent(
        new OcppMessagePersistedEvent(
            "CP-ADMIN-1", "session-1", "OUTBOUND", "BootNotification", "PARSED", "Accepted", Instant.now()));

    assertThat(channel.payloads)
        .extracting(AdminRealtimePayload::eventType)
        .containsExactly(
            "station.status.changed",
            "connector.changed",
            "telemetry.summary",
            "session.message.persisted");
  }

  @Test
  void relaysAdminCommandsWithUserIdentity() {
    CapturingMessageChannel channel = new CapturingMessageChannel();
    SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(channel);
    AdminCommandGateway gateway =
        (request, user) ->
            new AdminCommandResult(
                "cmd-1",
                request.stationId(),
                request.component(),
                request.action(),
                AdminCommandStatus.SENT,
                "queued",
                Instant.now());
    AdminWebSocketCommandController controller =
        new AdminWebSocketCommandController(messagingTemplate, gateway);

    Principal principal = () -> "ops-user";
    controller.relay(
        new AdminCommandRequest(
            "CP-ADMIN-2", "CHARGE_POINT", "Reset", "python-ocpp16", Map.of("type", "Soft")), principal);

    assertThat(channel.payloads).hasSize(1);
    AdminRealtimePayload payload = channel.payloads.getFirst();
    assertThat(payload.eventType()).isEqualTo("admin.command.result");
    assertThat(payload.stationId()).isEqualTo("CP-ADMIN-2");
    assertThat(payload.details()).containsEntry("commandId", "cmd-1");
    assertThat(payload.details()).containsEntry("action", "Reset");
    assertThat(payload.details()).containsEntry("status", AdminCommandStatus.SENT);
  }

  private static class CapturingMessageChannel implements MessageChannel {

    private final List<AdminRealtimePayload> payloads = new ArrayList<>();

    @Override
    public boolean send(Message<?> message) {
      return send(message, 0);
    }

    @Override
    public boolean send(Message<?> message, long timeout) {
      Object payload = message.getPayload();
      if (payload instanceof AdminRealtimePayload realtimePayload) {
        payloads.add(realtimePayload);
      }
      return true;
    }
  }
}
