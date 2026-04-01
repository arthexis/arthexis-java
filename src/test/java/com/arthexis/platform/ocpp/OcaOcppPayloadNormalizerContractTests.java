package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OcaOcppPayloadNormalizerContractTests {

  private static final Path FIXTURE_DIR = Path.of("docs/ocpp/fixtures");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private OcaOcppPayloadNormalizer normalizer;

  @BeforeEach
  void setUp() {
    normalizer = new OcaOcppPayloadNormalizer();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtureFiles")
  void keepsStationIdentityResolutionBackwardCompatible(String fixtureFile) throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    String resolved = normalizer.resolveStationId(stringValue(fixture.get("sessionId")), payload);

    assertThat(resolved).isEqualTo(stringValue(expected.get("resolvedStationId")));
    assertThat(resolved).isInstanceOf(String.class);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("meterValueFixtures")
  void keepsMeterValuesNormalizationContractBackwardCompatible(String fixtureFile) throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    String stationId = normalizer.resolveStationId(stringValue(fixture.get("sessionId")), payload);
    Map<String, Object> normalized = normalizer.normalizeMeterValues(stationId, payload);

    assertNormalizationMatchesContract(normalized, expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("transactionEventFixtures")
  void keepsTransactionEventNormalizationContractBackwardCompatible(String fixtureFile)
      throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    String stationId = normalizer.resolveStationId(stringValue(fixture.get("sessionId")), payload);
    Map<String, Object> normalized = normalizer.normalizeTransactionEvent(stationId, payload);

    assertNormalizationMatchesContract(normalized, expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("authorizeFixtures")
  void keepsAuthorizeNormalizationContractBackwardCompatible(String fixtureFile) throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    String stationId = normalizer.resolveStationId(stringValue(fixture.get("sessionId")), payload);
    Map<String, Object> normalized = normalizer.normalizeAuthorize(stationId, payload);

    assertNormalizationMatchesContract(normalized, expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("diagnosticsFixtures")
  void keepsDiagnosticsNormalizationContractBackwardCompatible(String fixtureFile) throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    String stationId = normalizer.resolveStationId(stringValue(fixture.get("sessionId")), payload);
    Map<String, Object> normalized = normalizer.normalizeDiagnosticsStatus(stationId, payload);

    assertNormalizationMatchesContract(normalized, expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("firmwareFixtures")
  void keepsFirmwareNormalizationContractBackwardCompatible(String fixtureFile) throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    String stationId = normalizer.resolveStationId(stringValue(fixture.get("sessionId")), payload);
    Map<String, Object> normalized = normalizer.normalizeFirmwareStatus(stationId, payload);

    assertNormalizationMatchesContract(normalized, expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("availabilityFixtures")
  void keepsAvailabilityNormalizationContractBackwardCompatible(String fixtureFile)
      throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    String stationId = normalizer.resolveStationId(stringValue(fixture.get("sessionId")), payload);
    Map<String, Object> normalized = normalizer.normalizeAvailabilityStatus(stationId, payload);

    assertNormalizationMatchesContract(normalized, expected);
  }

  private static Stream<Arguments> fixtureFiles() {
    return Stream.of(
            "boot_notification.ocpp16.json",
            "boot_notification.ocpp2x.json",
            "heartbeat.ocpp16.json",
            "heartbeat.ocpp2x.json",
            "status_notification.ocpp16.json",
            "status_notification.ocpp2x.json",
            "authorize.ocpp16.json",
            "diagnostics_status.ocpp16.json",
            "firmware_status.ocpp2x.json",
            "availability_status.ocpp2x.json",
            "meter_values.ocpp16.json",
            "meter_values.ocpp2x.json",
            "transaction_event.ocpp16.json",
            "transaction_event.ocpp2x.json",
            "start_transaction.ocpp16.json",
            "stop_transaction.ocpp16.json",
            "security_event_notification.ocpp2x.json",
            "notify_event.ocpp2x.json")
        .map(Arguments::of);
  }

  private static Stream<Arguments> meterValueFixtures() {
    return Stream.of("meter_values.ocpp16.json", "meter_values.ocpp2x.json").map(Arguments::of);
  }

  private static Stream<Arguments> transactionEventFixtures() {
    return Stream.of("transaction_event.ocpp16.json", "transaction_event.ocpp2x.json")
        .map(Arguments::of);
  }

  private static Stream<Arguments> authorizeFixtures() {
    return Stream.of("authorize.ocpp16.json").map(Arguments::of);
  }

  private static Stream<Arguments> diagnosticsFixtures() {
    return Stream.of("diagnostics_status.ocpp16.json").map(Arguments::of);
  }

  private static Stream<Arguments> firmwareFixtures() {
    return Stream.of("firmware_status.ocpp2x.json").map(Arguments::of);
  }

  private static Stream<Arguments> availabilityFixtures() {
    return Stream.of("availability_status.ocpp2x.json").map(Arguments::of);
  }

  private static void assertNormalizationMatchesContract(
      Map<String, Object> normalized, Map<String, Object> expected) {
    Map<String, Object> expectedNormalized = mapValue(expected.get("normalized"));
    Map<String, Object> expectedTypes = mapValue(expected.get("types"));
    List<Object> absentKeys = listValue(expected.get("absentKeys"));

    expectedNormalized.forEach(
        (key, value) -> {
          assertThat(normalized).containsKey(key);
          assertThat(normalized.get(key)).isEqualTo(value);
        });

    expectedTypes.forEach(
        (key, type) -> {
          Object actual = normalized.get(key);
          assertThat(actual).as("type check for %s", key).isNotNull();
          if ("string".equals(type)) {
            assertThat(actual).isInstanceOf(String.class);
          }
          if ("number".equals(type)) {
            assertThat(actual).isInstanceOf(Number.class);
          }
        });

    absentKeys.forEach(key -> assertThat(normalized).doesNotContainKey(stringValue(key)));
  }

  private static Map<String, Object> readFixture(String fixtureFile) throws IOException {
    String json = Files.readString(FIXTURE_DIR.resolve(fixtureFile));
    return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
  }

  private static Map<String, Object> mapValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> typed = (Map<String, Object>) map;
      return typed;
    }
    return Map.of();
  }

  private static List<Object> listValue(Object value) {
    if (value instanceof List<?> list) {
      return List.copyOf(list);
    }
    return List.of();
  }

  private static String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }
}
