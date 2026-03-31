package com.arthexis.platform.app.admin.api;

import com.arthexis.platform.app.admin.AdminCommandGateway;
import com.arthexis.platform.app.admin.AdminCommandRequest;
import com.arthexis.platform.app.admin.AdminCommandResult;
import com.arthexis.platform.charging.ChargingConnectorState;
import com.arthexis.platform.charging.ChargingConnectorStateRepository;
import com.arthexis.platform.charging.ChargingStation;
import com.arthexis.platform.charging.ChargingStationRepository;
import com.arthexis.platform.ocpp.OcppCommandRecord;
import com.arthexis.platform.ocpp.OcppCommandRecordRepository;
import com.arthexis.platform.ocpp.OcppMessageRecord;
import com.arthexis.platform.ocpp.OcppMessageRecordRepository;
import java.security.Principal;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api")
public class AdminApiController {

  private final ChargingStationRepository stationRepository;
  private final ChargingConnectorStateRepository connectorStateRepository;
  private final OcppCommandRecordRepository commandRepository;
  private final OcppMessageRecordRepository messageRepository;
  private final AdminCommandGateway adminCommandGateway;

  public AdminApiController(
      ChargingStationRepository stationRepository,
      ChargingConnectorStateRepository connectorStateRepository,
      OcppCommandRecordRepository commandRepository,
      OcppMessageRecordRepository messageRepository,
      AdminCommandGateway adminCommandGateway) {
    this.stationRepository = stationRepository;
    this.connectorStateRepository = connectorStateRepository;
    this.commandRepository = commandRepository;
    this.messageRepository = messageRepository;
    this.adminCommandGateway = adminCommandGateway;
  }

  @GetMapping(path = "/stations", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Map<String, Object>> stations(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = page(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
    return stationRepository.findAll(pageable).map(this::toStationItem);
  }

  @GetMapping(path = "/connectors", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Map<String, Object>> connectors(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = page(page, size, Sort.by(Sort.Direction.DESC, "lastStatusAt"));
    return connectorStateRepository.findAll(pageable).map(this::toConnectorItem);
  }

  @GetMapping(path = "/commands", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Map<String, Object>> commands(
      @RequestParam(required = false) String stationId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = page(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
    return (stationId == null || stationId.isBlank()
            ? commandRepository.findAll(pageable)
            : commandRepository.findByStationId(stationId, pageable))
        .map(this::toCommandItem);
  }

  @GetMapping(path = "/ocpp-messages", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<Map<String, Object>> messages(
      @RequestParam(required = false) String stationId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = page(page, size, Sort.by(Sort.Direction.DESC, "sampledAt"));
    return (stationId == null || stationId.isBlank()
            ? messageRepository.findAll(pageable)
            : messageRepository.findByStationId(stationId, pageable))
        .map(this::toMessageItem);
  }

  @PostMapping(path = "/commands", consumes = MediaType.APPLICATION_JSON_VALUE)
  public AdminCommandResult submit(@RequestBody AdminCommandRequest request, Principal principal) {
    String user = principal == null ? "unknown" : principal.getName();
    return adminCommandGateway.submit(request, user);
  }

  private Pageable page(int page, int size, Sort sort) {
    return PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100), sort);
  }

  private Map<String, Object> toStationItem(ChargingStation station) {
    java.util.LinkedHashMap<String, Object> item = new java.util.LinkedHashMap<>();
    item.put("stationId", station.getStationId());
    item.put("status", station.getStatus());
    item.put("previousStatus", "");
    item.put("tenantId", empty(station.getTenantId()));
    item.put("siteId", empty(station.getSiteId()));
    item.put("lastSeenAt", station.getLastSeenAt());
    item.put("updatedAt", station.getUpdatedAt());
    return item;
  }

  private Map<String, Object> toConnectorItem(ChargingConnectorState state) {
    java.util.LinkedHashMap<String, Object> item = new java.util.LinkedHashMap<>();
    item.put("stationId", state.getStationId());
    item.put("evseId", state.getEvseId());
    item.put("connectorId", state.getConnectorId());
    item.put("status", state.getConnectorStatus());
    item.put("connectorType", empty(state.getConnectorType()));
    item.put("availability", empty(state.getAvailability()));
    item.put("lastStatusAt", state.getLastStatusAt());
    return item;
  }

  private Map<String, Object> toCommandItem(OcppCommandRecord command) {
    java.util.LinkedHashMap<String, Object> item = new java.util.LinkedHashMap<>();
    item.put("commandId", command.getCommandId());
    item.put("stationId", command.getStationId());
    item.put("component", command.getComponent());
    item.put("action", command.getAction());
    item.put("status", command.getStatus());
    item.put("messageId", empty(command.getMessageId()));
    item.put("failureReason", empty(command.getFailureReason()));
    item.put("requestedBy", empty(command.getRequestedBy()));
    item.put("requestedAt", command.getRequestedAt());
    item.put("updatedAt", command.getUpdatedAt());
    return item;
  }

  private Map<String, Object> toMessageItem(OcppMessageRecord message) {
    java.util.LinkedHashMap<String, Object> item = new java.util.LinkedHashMap<>();
    item.put("sessionId", message.getSessionId());
    item.put("stationId", empty(message.getStationId()));
    item.put("direction", message.getDirection());
    item.put("action", empty(message.getAction()));
    item.put("messageId", empty(message.getMessageId()));
    item.put("parseStatus", message.getParseStatus());
    item.put("resultStatus", empty(message.getResultStatus()));
    item.put("sampledAt", message.getSampledAt());
    item.put("createdAt", message.getCreatedAt());
    return item;
  }

  private String empty(String value) {
    return value == null ? "" : value;
  }
}
