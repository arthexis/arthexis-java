package com.arthexis.platform.app.admin;

import com.arthexis.platform.telemetry.TelemetrySummaryEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminWebSocketDomainEventBridge {

  public static final String ADMIN_TOPIC_EVENTS = "/topic/admin/events";

  private final SimpMessagingTemplate messagingTemplate;

  public AdminWebSocketDomainEventBridge(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @EventListener
  public void onStationStatusChanged(StationStatusChangedEvent event) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("status", event.status());
    details.put("previousStatus", event.previousStatus());
    details.put("tenantId", event.tenantId());
    details.put("siteId", event.siteId());

    publish(new AdminRealtimePayload("station.status.changed", event.stationId(), event.occurredAt(), details));
  }

  @EventListener
  public void onConnectorChanged(ConnectorChangedEvent event) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("evseId", event.evseId());
    details.put("connectorId", event.connectorId());
    details.put("status", event.status());
    details.put("connectorType", event.connectorType());
    details.put("availability", event.availability());

    publish(new AdminRealtimePayload("connector.changed", event.stationId(), event.occurredAt(), details));
  }

  @EventListener
  public void onTelemetrySummary(TelemetrySummaryEvent event) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("totalSamples", event.totalSamples());
    details.put("structuredSamples", event.structuredSamples());
    details.put("numericPayloadSamples", event.numericPayloadSamples());
    details.put("sampledAt", event.sampledAt());

    publish(new AdminRealtimePayload("telemetry.summary", event.stationId(), event.occurredAt(), details));
  }

  @EventListener
  public void onSessionEvent(OcppMessagePersistedEvent event) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("sessionId", event.sessionId());
    details.put("direction", event.direction());
    details.put("action", event.action());
    details.put("parseStatus", event.parseStatus());
    details.put("resultStatus", event.resultStatus());

    publish(new AdminRealtimePayload("session.message.persisted", event.stationId(), event.occurredAt(), details));
  }


  @EventListener
  public void onCommandStatusChanged(OcppCommandStatusChangedEvent event) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("commandId", event.commandId());
    details.put("component", event.component());
    details.put("action", event.action());
    details.put("status", event.status());
    details.put("messageId", event.messageId());
    details.put("detail", event.detail());

    publish(new AdminRealtimePayload("ocpp.command.status", event.stationId(), event.occurredAt(), details));
  }

  private void publish(AdminRealtimePayload payload) {
    messagingTemplate.convertAndSend(ADMIN_TOPIC_EVENTS, payload);
  }
}
