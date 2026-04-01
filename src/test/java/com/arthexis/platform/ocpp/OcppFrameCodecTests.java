package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcppFrameCodecTests {

  private OcppFrameCodec codec;

  @BeforeEach
  void setUp() {
    codec = new OcppFrameCodec(new ObjectMapper());
  }

  @Test
  void decodesCallFrame() throws Exception {
    OcppMessage decoded = codec.decode("[2,\"msg-1\",\"Heartbeat\",{\"stationId\":\"CP-1\"}]");

    assertThat(decoded.messageType()).isEqualTo("CALL");
    assertThat(decoded.messageId()).isEqualTo("msg-1");
    assertThat(decoded.action()).isEqualTo("Heartbeat");
  }

  @Test
  void encodesCallResultAsArrayFrame() throws Exception {
    String encoded = codec.encode(new OcppMessage("CALLRESULT", "msg-1", null, Map.of("status", "Accepted")));

    assertThat(encoded).isEqualTo("[3,\"msg-1\",{\"status\":\"Accepted\"}]");
  }

  @Test
  void rejectsMalformedFrameLengths() {
    assertThatThrownBy(() -> codec.decode("[3,\"msg-1\",{},\"extra\"]"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("CALLRESULT frame must have 3 elements");
  }

  @Test
  void rejectsUnknownMessageTypeIds() {
    assertThatThrownBy(() -> codec.decode("[8,\"msg-1\",\"Ping\",{}]"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Unknown OCPP message type id");
  }
}
