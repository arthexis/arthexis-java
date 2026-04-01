package com.arthexis.platform.ocpp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OcppFrameCodec {

  private final ObjectMapper objectMapper;

  public OcppFrameCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public OcppMessage decode(String rawFrame) throws IOException {
    JsonNode root = objectMapper.readTree(rawFrame);
    if (!root.isArray()) {
      throw new IOException("OCPP frame must be a JSON array");
    }
    if (root.isEmpty()) {
      throw new IOException("OCPP frame cannot be empty");
    }

    JsonNode messageTypeNode = root.get(0);
    if (messageTypeNode == null || !messageTypeNode.isInt()) {
      throw new IOException("OCPP message type id must be an integer");
    }

    int messageTypeId = messageTypeNode.intValue();
    return switch (messageTypeId) {
      case 2 -> decodeCall(root);
      case 3 -> decodeCallResult(root);
      case 4 -> decodeCallError(root);
      default -> throw new IOException("Unknown OCPP message type id: " + messageTypeId);
    };
  }

  public String encode(OcppMessage message) throws JsonProcessingException {
    return objectMapper.writeValueAsString(toWireFrame(message));
  }

  private OcppMessage decodeCall(JsonNode root) throws IOException {
    if (root.size() != 4) {
      throw new IOException("CALL frame must have 4 elements");
    }
    String uniqueId = readText(root.get(1), "CALL uniqueId");
    String action = readText(root.get(2), "CALL action");
    Object payload = objectMapper.convertValue(root.get(3), Object.class);
    return new OcppMessage("CALL", uniqueId, action, payload);
  }

  private OcppMessage decodeCallResult(JsonNode root) throws IOException {
    if (root.size() != 3) {
      throw new IOException("CALLRESULT frame must have 3 elements");
    }
    String uniqueId = readText(root.get(1), "CALLRESULT uniqueId");
    Object payload = objectMapper.convertValue(root.get(2), Object.class);
    return new OcppMessage("CALLRESULT", uniqueId, null, payload);
  }

  private OcppMessage decodeCallError(JsonNode root) throws IOException {
    if (root.size() != 5) {
      throw new IOException("CALLERROR frame must have 5 elements");
    }
    String uniqueId = readText(root.get(1), "CALLERROR uniqueId");
    String errorCode = readText(root.get(2), "CALLERROR errorCode");
    String errorDescription = readText(root.get(3), "CALLERROR errorDescription");
    Object errorDetails = objectMapper.convertValue(root.get(4), Object.class);

    return new OcppMessage(
        "CALLERROR",
        uniqueId,
        null,
        Map.of(
            "errorCode", errorCode,
            "errorDescription", errorDescription,
            "errorDetails", errorDetails == null ? Map.of() : errorDetails));
  }

  private Object toWireFrame(OcppMessage message) {
    String type = normalizeType(message.messageType());
    return switch (type) {
      case "CALL" ->
          listOf(2, message.messageId(), requiredText(message.action(), "CALL action"), message.payload());
      case "CALLRESULT" -> listOf(3, message.messageId(), message.payload());
      case "CALLERROR" -> toCallErrorFrame(message);
      default -> throw new IllegalArgumentException("Unsupported OCPP message type: " + message.messageType());
    };
  }

  private List<?> toCallErrorFrame(OcppMessage message) {
    if (message.payload() instanceof Map<?, ?> payload) {
      @SuppressWarnings("unchecked")
      Map<String, Object> castedPayload = (Map<String, Object>) payload;
      Object errorCode = Objects.requireNonNullElse(castedPayload.get("errorCode"), "InternalError");
      Object errorDescription =
          Objects.requireNonNullElse(castedPayload.get("errorDescription"), "Unhandled error");
      Object errorDetails = Objects.requireNonNullElse(castedPayload.get("errorDetails"), Map.of());
      return listOf(4, message.messageId(), errorCode, errorDescription, errorDetails);
    }
    return listOf(
        4,
        message.messageId(),
        "InternalError",
        message.payload() == null ? "Unhandled error" : message.payload().toString(),
        Map.of());
  }

  private List<Object> listOf(Object... values) {
    return Arrays.asList(values);
  }

  private String normalizeType(String rawType) {
    if (rawType == null) {
      return "";
    }
    return switch (rawType) {
      case "2", "CALL" -> "CALL";
      case "3", "CALLRESULT" -> "CALLRESULT";
      case "4", "CALLERROR" -> "CALLERROR";
      default -> rawType;
    };
  }

  private String readText(JsonNode node, String label) throws IOException {
    if (node == null || !node.isTextual()) {
      throw new IOException(label + " must be a string");
    }
    return node.textValue();
  }

  private String requiredText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value;
  }
}
