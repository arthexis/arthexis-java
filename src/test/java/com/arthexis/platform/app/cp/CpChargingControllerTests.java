package com.arthexis.platform.app.cp;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arthexis.platform.security.SecurityConfig;
import com.arthexis.platform.security.MfaService;
import com.arthexis.platform.telemetry.TelemetrySample;
import com.arthexis.platform.telemetry.TelemetrySampleRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CpChargingController.class)
@Import({SecurityConfig.class, CpChargingRealtimeService.class, CpChargingControllerTests.TestUsers.class})
@ActiveProfiles("test")
class CpChargingControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockBean private TelemetrySampleRepository telemetrySampleRepository;
  @MockBean private MfaService mfaService;

  @Test
  void redirectsToCpChargingPage() throws Exception {
    mockMvc
        .perform(get("/cp/charging").with(httpBasic("sim-cp-001", "password")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cp/index.html"));
  }

  @Test
  void returnsHistoryForCpCustomers() throws Exception {
    when(telemetrySampleRepository.findTop120ByStationIdAndMetricNameOrderBySampledAtDesc(
            "sim-cp-001", "Power.Active.Import"))
        .thenReturn(
            List.of(
                new TelemetrySample(
                    "sim-cp-001",
                    "Power.Active.Import",
                    13.4,
                    "connector",
                    "evse-1:connector-1",
                    "kW",
                    null,
                    null,
                    null,
                    Instant.parse("2026-03-31T00:01:00Z")),
                new TelemetrySample(
                    "sim-cp-001",
                    "Power.Active.Import",
                    12.4,
                    "connector",
                    "evse-1:connector-1",
                    "kW",
                    null,
                    null,
                    null,
                    Instant.parse("2026-03-31T00:00:00Z"))));

    mockMvc
        .perform(
            get("/cp/charging/history")
                .with(httpBasic("sim-cp-001", "password"))
                .queryParam("stationId", "sim-cp-001")
                .queryParam("metricName", "Power.Active.Import"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].stationId").value("sim-cp-001"))
        .andExpect(jsonPath("$[0].metricName").value("Power.Active.Import"))
        .andExpect(jsonPath("$[0].metricValue").value(12.4))
        .andExpect(jsonPath("$[1].metricValue").value(13.4));
  }

  @Test
  void forbidsCpCustomerFromAccessingDifferentStation() throws Exception {
    mockMvc
        .perform(
            get("/cp/charging/history")
                .with(httpBasic("sim-cp-001", "password"))
                .queryParam("stationId", "sim-cp-002")
                .queryParam("metricName", "Power.Active.Import"))
        .andExpect(status().isForbidden());
  }

  @Test
  void deniesCpChargingDataToUnauthorizedRoles() throws Exception {
    mockMvc
        .perform(get("/cp/charging/history").with(httpBasic("viewer", "password")))
        .andExpect(status().isForbidden());
  }

  static class TestUsers {

    @SuppressWarnings("deprecation")
    @org.springframework.context.annotation.Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {
      return new InMemoryUserDetailsManager(
          org.springframework.security.core.userdetails.User.withUsername("sim-cp-001")
              .password("password")
              .roles("CP_CUSTOMER")
              .build(),
          org.springframework.security.core.userdetails.User.withUsername("viewer")
              .password("password")
              .roles("VIEWER")
              .build());
    }

    @SuppressWarnings("deprecation")
    @org.springframework.context.annotation.Bean
    PasswordEncoder passwordEncoder() {
      return NoOpPasswordEncoder.getInstance();
    }
  }
}
