package com.arthexis.platform.ocpp;

import com.arthexis.platform.app.admin.AdminCommandGateway;
import com.arthexis.platform.app.admin.AdminCommandRequest;
import com.arthexis.platform.app.admin.AdminCommandResult;
import com.arthexis.platform.app.admin.AdminCommandStatus;
import com.arthexis.platform.app.admin.OcppCommandStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcppCommandDispatchService implements AdminCommandGateway {

  private static final Set<String> PYTHON_TESTED_PROFILE_OCPP16_ACTIONS =
      Set.of("RemoteStartTransaction", "RemoteStopTransaction", "ChangeAvailability", "Reset");

  private static final Set<String> PYTHON_TESTED_PROFILE_OCPP2X_ACTIONS =
      Set.of("RequestStartTransaction", "RequestStopTransaction", "SetChargingProfile", "Reset");

  private final OcppCommandRecordRepository commandRepository;
  private final OcppOutboundSessionRouter sessionRouter;
  private final OcppSessionStateStore stateStore;
  private final OcppCommandDispatchProperties properties;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  public OcppCommandDispatchService(
      OcppCommandRecordRepository commandRepository,
      OcppOutboundSessionRouter sessionRouter,
      OcppSessionStateStore stateStore,
      OcppCommandDispatchProperties properties,
      ObjectMapper objectMapper,
      ApplicationEventPublisher eventPublisher) {
    this.commandRepository = commandRepository;
    this.sessionRouter = sessionRouter;
    this.stateStore = stateStore;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public AdminCommandResult submit(AdminCommandRequest request, String user) {
    if (request == null || blank(request.stationId()) || blank(request.action())) {
      return rejected(request, "stationId and action are required");
    }

    String component = blank(request.component()) ? "CHARGE_POINT" : request.component().trim();
    String profile = blank(request.chargerProfile()) ? "python-ocpp16" : request.chargerProfile().trim();

    if (!isSupportedForProfile(profile, request.action())) {
      String message =
          "Command action '%s' is not supported for charger profile '%s'"
              .formatted(request.action(), profile);
      publish(
          new OcppCommandStatusChangedEvent(
              null,
              request.stationId(),
              component,
              request.action(),
              AdminCommandStatus.FAILED,
              null,
              message,
              Instant.now()));
      return new AdminCommandResult(
          null,
          request.stationId(),
          component,
          request.action(),
          AdminCommandStatus.FAILED,
          message,
          Instant.now());
    }

    OcppCommandRecord command =
        new OcppCommandRecord(
            request.stationId().trim(),
            component,
            request.action().trim(),
            toJson(request.payload() == null ? Map.of() : request.payload()),
            user,
            Math.max(1, properties.getMaxRetries() + 1));

    Instant now = Instant.now();
    command.markQueued(now, now);
    OcppCommandRecord persisted = commandRepository.save(command);
    stateStore.storePendingCommand(
        persisted.getStationId(), persisted.getCommandId(), commandKey(persisted));
    publishFromRecord(persisted, "Command queued for dispatch");

    dispatchCommand(persisted);

    return new AdminCommandResult(
        persisted.getCommandId(),
        persisted.getStationId(),
        persisted.getComponent(),
        persisted.getAction(),
        toAdminStatus(persisted.getStatus()),
        "Command accepted",
        Instant.now());
  }

  @Transactional
  public void acknowledgeByMessageId(String messageId) {
    if (blank(messageId)) {
      return;
    }
    commandRepository
        .findByMessageIdAndStatus(messageId, OcppCommandStatus.SENT)
        .ifPresent(
            command -> {
              command.markAcknowledged(Instant.now());
              commandRepository.save(command);
              publishFromRecord(command, "Charger acknowledged command");
            });
  }

  @Scheduled(fixedDelayString = "${arthexis.ocpp.commands.scheduler-delay-ms:5000}")
  @Transactional
  public void processQueue() {
    Instant now = Instant.now();
    List<OcppCommandRecord> dueCommands =
        commandRepository.findByStatusInAndNextAttemptAtLessThanEqual(
            List.of(OcppCommandStatus.QUEUED, OcppCommandStatus.SENT), now);

    for (OcppCommandRecord command : dueCommands) {
      if (command.getStatus() == OcppCommandStatus.SENT) {
        handleSentTimeout(command);
      } else {
        dispatchCommand(command);
      }
    }
  }

  private void handleSentTimeout(OcppCommandRecord command) {
    Instant now = Instant.now();
    if (command.getAttemptCount() >= command.getMaxAttempts()) {
      command.markTimedOut(now, "No acknowledgement received before timeout");
      commandRepository.save(command);
      publishFromRecord(command, command.getFailureReason());
      return;
    }

    command.markQueued(now, now.plus(properties.getRetryDelay()));
    commandRepository.save(command);
    publishFromRecord(command, "Ack timeout reached; command re-queued");
  }

  private void dispatchCommand(OcppCommandRecord command) {
    Instant now = Instant.now();
    if (command.getAttemptCount() >= command.getMaxAttempts()) {
      command.markFailed(now, "Retry budget exhausted before dispatch");
      commandRepository.save(command);
      publishFromRecord(command, command.getFailureReason());
      return;
    }

    String messageId = UUID.randomUUID().toString();
    try {
      Map payload = objectMapper.readValue(command.getPayloadJson(), Map.class);
      OcppMessage outbound = new OcppMessage("CALL", messageId, command.getAction(), payload);
      sessionRouter.sendToStation(command.getStationId(), outbound);
      command.markSent(messageId, now, now.plus(properties.getAckTimeout()));
      commandRepository.save(command);
      stateStore.storePendingCommand(command.getStationId(), command.getCommandId(), commandKey(command));
      publishFromRecord(command, "Command dispatched to station websocket session");
    } catch (JsonProcessingException ex) {
      command.markFailed(now, "Invalid command payload JSON");
      commandRepository.save(command);
      publishFromRecord(command, command.getFailureReason());
    } catch (IOException ex) {
      command.markQueued(now, now.plus(properties.getRetryDelay()));
      commandRepository.save(command);
      publishFromRecord(command, "Dispatch deferred: " + ex.getMessage());
    }
  }

  private String commandKey(OcppCommandRecord command) {
    return command.getComponent() + ":" + command.getAction();
  }

  private boolean isSupportedForProfile(String profile, String action) {
    if (blank(action)) {
      return false;
    }
    String normalized = profile.toLowerCase();
    Set<String> supported =
        normalized.contains("2") ? PYTHON_TESTED_PROFILE_OCPP2X_ACTIONS : PYTHON_TESTED_PROFILE_OCPP16_ACTIONS;
    return supported.contains(action);
  }

  private AdminCommandResult rejected(AdminCommandRequest request, String message) {
    String station = request == null ? "unknown" : request.stationId();
    String component = request == null ? "unknown" : request.component();
    String action = request == null ? "unknown" : request.action();
    publish(
        new OcppCommandStatusChangedEvent(
            null,
            station,
            component,
            action,
            AdminCommandStatus.FAILED,
            null,
            message,
            Instant.now()));
    return new AdminCommandResult(
        null, station, component, action, AdminCommandStatus.FAILED, message, Instant.now());
  }

  private void publishFromRecord(OcppCommandRecord command, String detail) {
    publish(
        new OcppCommandStatusChangedEvent(
            command.getCommandId(),
            command.getStationId(),
            command.getComponent(),
            command.getAction(),
            toAdminStatus(command.getStatus()),
            command.getMessageId(),
            detail,
            Instant.now()));
  }

  private void publish(OcppCommandStatusChangedEvent event) {
    eventPublisher.publishEvent(event);
  }

  private String toJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      return "{}";
    }
  }


  private AdminCommandStatus toAdminStatus(OcppCommandStatus status) {
    return AdminCommandStatus.valueOf(status.name());
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
