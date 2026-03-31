package com.arthexis.platform.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "arthexis.security.auth.mode=basic")
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AdminMfaIntegrationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void deniesPrivilegedRouteWithoutSecondFactor() throws Exception {
    mockMvc
        .perform(post("/admin/commands/dispatch").with(httpBasic("admin", "password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("mfa_required"));
  }

  @Test
  void allowsStepUpAfterTotpVerification() throws Exception {
    MockHttpSession session = new MockHttpSession();

    MvcResult enrollResult =
        mockMvc
            .perform(
                post("/auth/mfa/totp/enroll")
                    .with(httpBasic("admin", "password"))
                    .session(session))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode enrollPayload = objectMapper.readTree(enrollResult.getResponse().getContentAsString());
    String secret = enrollPayload.get("secret").asText();
    String code = totpCode(secret, Instant.now().getEpochSecond() / 30);

    mockMvc
        .perform(
            post("/auth/mfa/totp/verify-enrollment")
                .with(httpBasic("admin", "password"))
                .session(session)
                .contentType("application/json")
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/auth/mfa/totp/verify")
                .with(httpBasic("admin", "password"))
                .session(session)
                .contentType("application/json")
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("mfa_verified"));

    mockMvc
        .perform(
            post("/admin/commands/dispatch")
                .with(httpBasic("admin", "password"))
                .session(session))
        .andExpect(status().isNotFound());
  }

  @Test
  void allowsStepUpAfterWebauthnAssertion() throws Exception {
    MockHttpSession session = new MockHttpSession();
    MvcResult options =
        mockMvc
            .perform(
                post("/auth/mfa/webauthn/register/options")
                    .with(httpBasic("operator", "password"))
                    .session(session))
            .andExpect(status().isOk())
            .andReturn();
    String registerChallenge =
        objectMapper.readTree(options.getResponse().getContentAsString()).get("challenge").asText();

    mockMvc
        .perform(
            post("/auth/mfa/webauthn/register/verify")
                .with(httpBasic("operator", "password"))
                .session(session)
                .contentType("application/json")
                .content(
                    "{\"challenge\":\""
                        + registerChallenge
                        + "\",\"credentialId\":\"cred-1\",\"publicKeyCose\":\"pk\",\"signCount\":1}"))
        .andExpect(status().isOk());

    MvcResult authOptions =
        mockMvc
            .perform(
                post("/auth/mfa/webauthn/authenticate/options")
                    .with(httpBasic("operator", "password"))
                    .session(session))
            .andExpect(status().isOk())
            .andReturn();
    String authChallenge =
        objectMapper.readTree(authOptions.getResponse().getContentAsString()).get("challenge").asText();

    mockMvc
        .perform(
            post("/auth/mfa/webauthn/authenticate/verify")
                .with(httpBasic("operator", "password"))
                .session(session)
                .contentType("application/json")
                .content(
                    "{\"challenge\":\""
                        + authChallenge
                        + "\",\"credentialId\":\"cred-1\",\"signCount\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("mfa_verified"));

    mockMvc
        .perform(
            post("/admin/commands/dispatch")
                .with(httpBasic("operator", "password"))
                .session(session))
        .andExpect(status().isNotFound());
  }

  private static String totpCode(String secret, long counter) throws Exception {
    byte[] key = decodeBase32(secret);
    byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(new SecretKeySpec(key, "HmacSHA1"));
    byte[] hash = mac.doFinal(msg);
    int offset = hash[hash.length - 1] & 0x0F;
    int binary =
        ((hash[offset] & 0x7F) << 24)
            | ((hash[offset + 1] & 0xFF) << 16)
            | ((hash[offset + 2] & 0xFF) << 8)
            | (hash[offset + 3] & 0xFF);
    return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
  }

  private static byte[] decodeBase32(String input) {
    final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    String normalized = input.replace("=", "").toUpperCase(Locale.ROOT);
    ByteBuffer buffer = ByteBuffer.allocate((normalized.length() * 5) / 8 + 8);
    int bitsLeft = 0;
    int value = 0;
    for (char c : normalized.toCharArray()) {
      int idx = alphabet.indexOf(c);
      if (idx < 0) {
        continue;
      }
      value = (value << 5) | idx;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        buffer.put((byte) ((value >> (bitsLeft - 8)) & 0xFF));
        bitsLeft -= 8;
      }
    }
    byte[] out = new byte[buffer.position()];
    buffer.flip();
    buffer.get(out);
    return out;
  }

  @TestConfiguration
  static class TestUsers {

    @SuppressWarnings("deprecation")
    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {
      return new InMemoryUserDetailsManager(
          User.withUsername("admin").password("password").roles("ADMIN").build(),
          User.withUsername("operator").password("password").roles("OPERATOR").build());
    }

    @SuppressWarnings("deprecation")
    @Bean
    PasswordEncoder passwordEncoder() {
      return NoOpPasswordEncoder.getInstance();
    }
  }
}
