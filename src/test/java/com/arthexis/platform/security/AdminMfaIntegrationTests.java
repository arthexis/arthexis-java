package com.arthexis.platform.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arthexis.platform.app.admin.AdminCommandResult;
import com.arthexis.platform.app.admin.AdminCommandStatus;
import com.arthexis.platform.ocpp.OcppCommandDispatchService;
import java.nio.ByteBuffer;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"h2", "test"})
class AdminMfaIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @MockBean private OcppCommandDispatchService commandDispatchService;

  @BeforeEach
  void setup() {
    when(commandDispatchService.submit(any(), anyString()))
        .thenReturn(
            new AdminCommandResult(
                "cmd-1",
                "station-1",
                "smartCharging",
                "setChargingProfile",
                AdminCommandStatus.ACKNOWLEDGED,
                "ok",
                Instant.now()));
  }

  @Test
  void sensitiveCommandWithoutStepUpTokenFails() throws Exception {
    mockMvc
        .perform(
            post("/admin/api/commands")
                .with(user("alice").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"stationId":"station-1","component":"smartCharging","action":"setChargingProfile","payload":{}}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("second_factor_required"));
  }

  @Test
  void totpStepUpAllowsSensitiveCommand() throws Exception {
    String enrollPayload =
        mockMvc
            .perform(post("/security/mfa/totp/enroll").with(user("alice").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String secret = new com.fasterxml.jackson.databind.ObjectMapper().readTree(enrollPayload).get("secret").asText();
    String otpauthUri =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(enrollPayload).get("otpauthUri").asText();
    org.assertj.core.api.Assertions.assertThat(otpauthUri).contains("otpauth://totp/Arthexis%3Aalice");
    String code = totp(secret, Instant.now());

    String verifyPayload =
        mockMvc
            .perform(
                post("/security/mfa/totp/verify")
                    .with(user("alice").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String stepUpToken =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(verifyPayload).get("stepUpToken").asText();

    mockMvc
        .perform(
            post("/admin/api/commands")
                .with(user("alice").roles("ADMIN"))
                .header(MfaService.STEP_UP_HEADER, stepUpToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"stationId":"station-1","component":"smartCharging","action":"setChargingProfile","payload":{}}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.commandId").value("cmd-1"));
  }

  @Test
  void invalidTotpCodeIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/security/mfa/totp/verify")
                .with(user("alice").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
        .andExpect(status().isBadRequest());
  }

  private String totp(String secret, Instant instant) throws Exception {
    byte[] key = decodeBase32(secret);
    long counter = instant.getEpochSecond() / 30L;
    ByteBuffer message = ByteBuffer.allocate(8).putLong(counter);
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(new SecretKeySpec(key, "HmacSHA1"));
    byte[] hash = mac.doFinal(message.array());
    int offset = hash[hash.length - 1] & 0x0F;
    int binary =
        ((hash[offset] & 0x7F) << 24)
            | ((hash[offset + 1] & 0xFF) << 16)
            | ((hash[offset + 2] & 0xFF) << 8)
            | (hash[offset + 3] & 0xFF);
    int otp = binary % 1_000_000;
    return String.format("%06d", otp);
  }

  private byte[] decodeBase32(String value) {
    String normalized = value.replace("=", "").toUpperCase();
    byte[] decoded = new byte[(normalized.length() * 5) / 8];
    int buffer = 0;
    int bitsLeft = 0;
    int outputIndex = 0;
    for (int i = 0; i < normalized.length(); i++) {
      char current = normalized.charAt(i);
      int base32Value;
      if (current >= 'A' && current <= 'Z') {
        base32Value = current - 'A';
      } else if (current >= '2' && current <= '7') {
        base32Value = current - '2' + 26;
      } else {
        throw new IllegalArgumentException("Invalid base32 value");
      }
      buffer = (buffer << 5) | base32Value;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        decoded[outputIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
        bitsLeft -= 8;
      }
    }
    return decoded;
  }

  @Test
  void webAuthnStepUpAllowsSensitiveCommand() throws Exception {
    String optionsPayload =
        mockMvc
            .perform(post("/security/mfa/webauthn/register/options").with(user("alice").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String challenge =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(optionsPayload).get("challenge").asText();

    mockMvc
        .perform(
            post("/security/mfa/webauthn/register/finish")
                .with(user("alice").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"alice","challenge":"%s","credentialId":"cred-1","publicKeyCose":"pk","signCount":1,"transports":"internal"}
                    """
                        .formatted(challenge)))
        .andExpect(status().isNoContent());

    String assertOptions =
        mockMvc
            .perform(post("/security/mfa/webauthn/assert/options").with(user("alice").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String assertChallenge =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(assertOptions).get("challenge").asText();

    String assertFinish =
        mockMvc
            .perform(
                post("/security/mfa/webauthn/assert/finish")
                    .with(user("alice").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"alice","challenge":"%s","credentialId":"cred-1","signCount":2}
                        """
                            .formatted(assertChallenge)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String stepUpToken =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(assertFinish).get("stepUpToken").asText();

    mockMvc
        .perform(
            post("/admin/api/commands")
                .with(user("alice").roles("ADMIN"))
                .header(MfaService.STEP_UP_HEADER, stepUpToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"stationId":"station-1","component":"smartCharging","action":"setChargingProfile","payload":{}}
                    """))
        .andExpect(status().isOk());
  }

  @Test
  void webAuthnRegistrationRejectsUsernameMismatch() throws Exception {
    String optionsPayload =
        mockMvc
            .perform(post("/security/mfa/webauthn/register/options").with(user("alice").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String challenge =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(optionsPayload).get("challenge").asText();

    mockMvc
        .perform(
            post("/security/mfa/webauthn/register/finish")
                .with(user("alice").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"mallory","challenge":"%s","credentialId":"cred-1","publicKeyCose":"pk","signCount":1,"transports":"internal"}
                    """
                        .formatted(challenge)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void webAuthnAssertionRejectsNonIncreasingSignCounter() throws Exception {
    String optionsPayload =
        mockMvc
            .perform(post("/security/mfa/webauthn/register/options").with(user("alice").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String challenge =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(optionsPayload).get("challenge").asText();

    mockMvc
        .perform(
            post("/security/mfa/webauthn/register/finish")
                .with(user("alice").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"alice","challenge":"%s","credentialId":"cred-counter","publicKeyCose":"pk","signCount":10,"transports":"internal"}
                    """
                        .formatted(challenge)))
        .andExpect(status().isNoContent());

    String assertOptions =
        mockMvc
            .perform(post("/security/mfa/webauthn/assert/options").with(user("alice").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String assertChallenge =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(assertOptions).get("challenge").asText();

    mockMvc
        .perform(
            post("/security/mfa/webauthn/assert/finish")
                .with(user("alice").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"alice","challenge":"%s","credentialId":"cred-counter","signCount":10}
                    """
                        .formatted(assertChallenge)))
        .andExpect(status().isBadRequest());
  }

}
