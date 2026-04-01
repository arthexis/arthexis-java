package com.arthexis.platform.operations.admin.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arthexis.platform.app.admin.AdminCommandGateway;
import com.arthexis.platform.app.admin.AdminCommandRequest;
import com.arthexis.platform.app.admin.AdminCommandResult;
import com.arthexis.platform.app.admin.AdminCommandStatus;
import com.arthexis.platform.charging.ChargingConnectorState;
import com.arthexis.platform.charging.ChargingConnectorStateRepository;
import com.arthexis.platform.charging.ChargingStation;
import com.arthexis.platform.charging.ChargingStationAdminDetails;
import com.arthexis.platform.charging.ChargingStationRepository;
import com.arthexis.platform.ocpp.OcppCommandRecordRepository;
import com.arthexis.platform.ocpp.OcppMessageRecordRepository;
import com.arthexis.platform.security.MfaService;
import com.arthexis.platform.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminApiController.class, AdminWebController.class})
@Import({SecurityConfig.class, AdminApiControllerTests.TestUsers.class})
@ActiveProfiles("test")
class AdminApiControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockBean private ChargingStationRepository stationRepository;
  @MockBean private ChargingConnectorStateRepository connectorStateRepository;
  @MockBean private OcppCommandRecordRepository commandRepository;
  @MockBean private OcppMessageRecordRepository messageRepository;
  @MockBean private AdminCommandGateway adminCommandGateway;
  @MockBean private MfaService mfaService;

  @Test
  void redirectsAdminRootToStaticIndex() throws Exception {
    mockMvc
        .perform(get("/admin").with(httpBasic("ops-admin", "password")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/index.html"));
  }

  @Test
  void servesStationHistoryForAdminRole() throws Exception {
    ChargingStation station = new ChargingStation("CP-ADMIN-1", "AVAILABLE");
    station.applyAdminDetails(
        new ChargingStationAdminDetails("A", "V", "M", "1.6", "f1", "t1", "s1", true, null, null));

    when(stationRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(station), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/admin/api/stations").with(httpBasic("ops-admin", "password")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].stationId").value("CP-ADMIN-1"))
        .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
  }

  @Test
  void servesConnectorHistoryForOperatorRole() throws Exception {
    when(connectorStateRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new ChargingConnectorState(
                        "CP-ADMIN-1", 1, 2, "CHARGING", "CCS2", "OPERATIVE", Instant.now())),
                PageRequest.of(0, 20),
                1));

    mockMvc
        .perform(get("/admin/api/connectors").with(httpBasic("ops-operator", "password")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].stationId").value("CP-ADMIN-1"))
        .andExpect(jsonPath("$.content[0].status").value("CHARGING"));
  }

  @Test
  void allowsAdminToSubmitCommandsUsingExistingContracts() throws Exception {
    when(mfaService.hasValidStepUp(eq("ops-admin"), any())).thenReturn(true);
    when(adminCommandGateway.submit(any(AdminCommandRequest.class), eq("ops-admin")))
        .thenReturn(
            new AdminCommandResult(
                "cmd-100",
                "CP-ADMIN-1",
                "CHARGE_POINT",
                "Reset",
                AdminCommandStatus.SENT,
                "queued",
                Instant.parse("2026-03-31T00:00:00Z")));

    mockMvc
        .perform(
            post("/admin/api/commands")
                .with(httpBasic("ops-admin", "password"))
                .header("X-Step-Up-Token", "step-up-token")
                .contentType("application/json")
                .content(
                    """
                    {
                      "stationId":"CP-ADMIN-1",
                      "component":"CHARGE_POINT",
                      "action":"Reset",
                      "chargerProfile":"python-ocpp16",
                      "payload":{"type":"Soft"}
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.commandId").value("cmd-100"))
        .andExpect(jsonPath("$.status").value("SENT"));
  }

  @Test
  void deniesAdminEndpointsToUnauthorizedRoles() throws Exception {
    mockMvc
        .perform(get("/admin/api/stations").with(httpBasic("viewer", "password")))
        .andExpect(status().isForbidden());
  }

  @Test
  void protectsAdminStaticPathForUnauthorizedRoles() throws Exception {
    mockMvc
        .perform(get("/admin/index.html").with(httpBasic("viewer", "password")))
        .andExpect(status().isForbidden());
  }

  static class TestUsers {

    @SuppressWarnings("deprecation")
    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {
      return new InMemoryUserDetailsManager(
          org.springframework.security.core.userdetails.User.withUsername("ops-admin")
              .password("password")
              .roles("ADMIN")
              .build(),
          org.springframework.security.core.userdetails.User.withUsername("ops-operator")
              .password("password")
              .roles("OPERATOR")
              .build(),
          org.springframework.security.core.userdetails.User.withUsername("viewer")
              .password("password")
              .roles("VIEWER")
              .build());
    }

    @SuppressWarnings("deprecation")
    @Bean
    PasswordEncoder passwordEncoder() {
      return NoOpPasswordEncoder.getInstance();
    }
  }
}
