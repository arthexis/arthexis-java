package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arthexis.platform.app.admin.AdminCommandRequest;
import com.arthexis.platform.app.admin.AdminCommandStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OcppCommandDispatchServiceTests {

  private static class NoopStateStore extends OcppSessionStateStore {

    NoopStateStore() {
      super(new org.springframework.data.redis.core.StringRedisTemplate());
    }

    @Override
    public void storePendingCommand(String stationId, String commandId, String action) {}

    @Override
    public void bindStationSession(String stationId, String sessionId) {}
  }

  @Mock private OcppCommandRecordRepository commandRepository;
  private OcppOutboundSessionRouter sessionRouter;
  private OcppSessionStateStore stateStore;
  @Mock private ApplicationEventPublisher eventPublisher;

  private OcppCommandDispatchService service;

  @BeforeEach
  void setUp() {
    OcppCommandDispatchProperties properties = new OcppCommandDispatchProperties();
    properties.setAckTimeout(Duration.ofSeconds(45));
    properties.setRetryDelay(Duration.ofSeconds(1));
    properties.setMaxRetries(3);

    sessionRouter = new OcppOutboundSessionRouter(new ObjectMapper());

    stateStore = new NoopStateStore();

    service =
        new OcppCommandDispatchService(
            commandRepository,
            sessionRouter,
            stateStore,
            properties,
            new ObjectMapper(),
            eventPublisher);
  }

  @Test
  void rejectsUnsupportedCommandsForPythonProfile() {
    AdminCommandRequest request =
        new AdminCommandRequest(
            "CP-16", "EVSE", "RequestStartTransaction", "python-ocpp16", Map.of());

    var result = service.submit(request, "admin");

    assertThat(result.status()).isEqualTo(AdminCommandStatus.FAILED);
    verify(commandRepository, never()).save(any());
  }

  @Test
  void supportsCustomProfileCapabilitiesFromProperties() {
    OcppCommandDispatchProperties properties = new OcppCommandDispatchProperties();
    properties.setAckTimeout(Duration.ofSeconds(45));
    properties.setRetryDelay(Duration.ofSeconds(1));
    properties.setMaxRetries(1);
    Map<String, Set<String>> custom = new LinkedHashMap<>();
    custom.put("new-profile", Set.of("TriggerMessage"));
    properties.setProfileCapabilities(custom);

    OcppCommandDispatchService profileAwareService =
        new OcppCommandDispatchService(
            commandRepository,
            sessionRouter,
            stateStore,
            properties,
            new ObjectMapper(),
            eventPublisher);

    AdminCommandRequest request =
        new AdminCommandRequest("CP-77", "EVSE", "TriggerMessage", "new-profile", Map.of());
    when(commandRepository.save(any(OcppCommandRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = profileAwareService.submit(request, "admin");

    assertThat(result.status()).isNotEqualTo(AdminCommandStatus.FAILED);
    verify(commandRepository, atLeast(1)).save(any(OcppCommandRecord.class));
  }

  @Test
  void keepsDefaultProfilesWhenCustomCapabilitiesAreConfigured() {
    OcppCommandDispatchProperties properties = new OcppCommandDispatchProperties();
    properties.setAckTimeout(Duration.ofSeconds(45));
    properties.setRetryDelay(Duration.ofSeconds(1));
    properties.setMaxRetries(1);
    properties.setProfileCapabilities(Map.of("new-profile", Set.of("TriggerMessage")));

    OcppCommandDispatchService profileAwareService =
        new OcppCommandDispatchService(
            commandRepository,
            sessionRouter,
            stateStore,
            properties,
            new ObjectMapper(),
            eventPublisher);

    AdminCommandRequest defaultProfileRequest =
        new AdminCommandRequest("CP-16", "EVSE", "Reset", "python-ocpp16", Map.of("type", "Soft"));
    when(commandRepository.save(any(OcppCommandRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = profileAwareService.submit(defaultProfileRequest, "admin");

    assertThat(result.status()).isNotEqualTo(AdminCommandStatus.FAILED);
    verify(commandRepository, atLeast(1)).save(any(OcppCommandRecord.class));
  }

  @Test
  void marksCommandAcknowledgedByMessageId() {
    OcppCommandRecord command =
        new OcppCommandRecord("CP-16", "EVSE", "Reset", "{}", "admin", 3);
    command.markQueued(Instant.now(), Instant.now());
    command.markSent("msg-1", Instant.now(), Instant.now().plusSeconds(20));

    when(commandRepository.findByMessageIdAndStatus("msg-1", OcppCommandStatus.SENT))
        .thenReturn(Optional.of(command));

    service.acknowledgeByMessageId("msg-1");

    ArgumentCaptor<OcppCommandRecord> captor = ArgumentCaptor.forClass(OcppCommandRecord.class);
    verify(commandRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(OcppCommandStatus.ACKNOWLEDGED);
  }

  @Test
  void requeuesSentCommandsWhenAckTimesOut() {
    OcppCommandRecord command =
        new OcppCommandRecord("CP-16", "EVSE", "Reset", "{}", "admin", 3);
    command.markQueued(Instant.now(), Instant.now());
    command.markSent("msg-2", Instant.now().minusSeconds(60), Instant.now().minusSeconds(1));

    when(commandRepository.findByStatusInAndNextAttemptAtLessThanEqual(
            eq(List.of(OcppCommandStatus.QUEUED, OcppCommandStatus.SENT)), any()))
        .thenReturn(List.of(command));

    service.processQueue();

    verify(commandRepository).save(command);
    assertThat(command.getStatus()).isEqualTo(OcppCommandStatus.QUEUED);
  }

  @Test
  void marksCommandFailedByMessageId() {
    OcppCommandRecord command =
        new OcppCommandRecord("CP-16", "EVSE", "Reset", "{}", "admin", 3);
    command.markQueued(Instant.now(), Instant.now());
    command.markSent("msg-9", Instant.now(), Instant.now().plusSeconds(20));

    when(commandRepository.findByMessageIdAndStatus("msg-9", OcppCommandStatus.SENT))
        .thenReturn(Optional.of(command));

    service.failByMessageId("msg-9", "ProtocolError: rejected");

    ArgumentCaptor<OcppCommandRecord> captor = ArgumentCaptor.forClass(OcppCommandRecord.class);
    verify(commandRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(OcppCommandStatus.FAILED);
    assertThat(captor.getValue().getFailureReason()).isEqualTo("ProtocolError: rejected");
  }

  @Test
  void queuesCommandWhenSessionIsUnavailable() {
    AdminCommandRequest request =
        new AdminCommandRequest("CP-16", "EVSE", "Reset", "python-ocpp16", Map.of("type", "Soft"));
    when(commandRepository.save(any(OcppCommandRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.submit(request, "admin");

    verify(commandRepository, atLeast(1)).save(any(OcppCommandRecord.class));
  }

  @Test
  void allowsInitialDispatchWhenMaxRetriesIsZero() {
    OcppCommandDispatchProperties properties = new OcppCommandDispatchProperties();
    properties.setAckTimeout(Duration.ofSeconds(45));
    properties.setRetryDelay(Duration.ofSeconds(1));
    properties.setMaxRetries(0);

    OcppCommandDispatchService zeroRetryService =
        new OcppCommandDispatchService(
            commandRepository,
            sessionRouter,
            stateStore,
            properties,
            new ObjectMapper(),
            eventPublisher);

    AdminCommandRequest request =
        new AdminCommandRequest("CP-16", "EVSE", "Reset", "python-ocpp16", Map.of("type", "Soft"));
    when(commandRepository.save(any(OcppCommandRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = zeroRetryService.submit(request, "admin");

    assertThat(result.commandId()).isNotNull();
    verify(commandRepository, atLeast(1)).save(any(OcppCommandRecord.class));
  }

  @Test
  void failsCommandImmediatelyWhenPayloadJsonIsInvalid() {
    OcppCommandRecord command =
        new OcppCommandRecord("CP-16", "EVSE", "Reset", "{bad json", "admin", 3);
    command.markQueued(Instant.now(), Instant.now());

    when(commandRepository.findByStatusInAndNextAttemptAtLessThanEqual(
            eq(List.of(OcppCommandStatus.QUEUED, OcppCommandStatus.SENT)), any()))
        .thenReturn(List.of(command));

    service.processQueue();

    assertThat(command.getStatus()).isEqualTo(OcppCommandStatus.FAILED);
    assertThat(command.getFailureReason()).isEqualTo("Invalid command payload JSON");
    verify(commandRepository).save(command);
  }
}
