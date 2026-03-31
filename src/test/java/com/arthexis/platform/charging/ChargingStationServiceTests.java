package com.arthexis.platform.charging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChargingStationServiceTests {

  @Mock private ChargingStationRepository repository;

  private ChargingStationService service;

  @BeforeEach
  void setUp() {
    service = new ChargingStationService(repository);
  }

  @Test
  void upsertStatusUsesDefaultWhenStatusIsMissing() {
    when(repository.findByStationId("CP-1")).thenReturn(Optional.empty());
    when(repository.save(any(ChargingStation.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ChargingStation station = service.upsertStatus("CP-1", "");

    assertThat(station.getStationId()).isEqualTo("CP-1");
    assertThat(station.getStatus()).isEqualTo("ONLINE");
    assertThat(station.isEnabled()).isTrue();
    verify(repository).save(any(ChargingStation.class));
  }

  @Test
  void upsertStatusDoesNotEraseExistingAdminFieldsWhenNewDetailsAreAbsent() {
    ChargingStation existing = new ChargingStation("CP-2", "AVAILABLE");
    existing.applyAdminDetails(
        new ChargingStationAdminDetails(
            "Main Depot",
            "Arthexis",
            "AX-50",
            "2.0.1",
            "1.2.3",
            "tenant-a",
            "site-1",
            false,
            Instant.parse("2026-03-31T00:00:00Z"),
            Instant.parse("2026-03-30T22:00:00Z")));

    when(repository.findByStationId("CP-2")).thenReturn(Optional.of(existing));
    when(repository.save(any(ChargingStation.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ChargingStation station = service.upsertStatus("CP-2", "CHARGING");

    assertThat(station.getStatus()).isEqualTo("CHARGING");
    assertThat(station.getDisplayName()).isEqualTo("Main Depot");
    assertThat(station.getVendor()).isEqualTo("Arthexis");
    assertThat(station.getTenantId()).isEqualTo("tenant-a");
    assertThat(station.getSiteId()).isEqualTo("site-1");
    assertThat(station.isEnabled()).isFalse();
  }
}
