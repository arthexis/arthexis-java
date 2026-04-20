package com.arthexis.platform.ocpp;

import java.time.Duration;
import java.util.OptionalInt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcppSessionStateStore {

  private static final Duration SESSION_TTL = Duration.ofHours(12);
  private final StringRedisTemplate redisTemplate;

  public OcppSessionStateStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void storePendingCommand(String stationId, String commandId, String action) {
    String key = "ocpp:pending:" + stationId + ":" + commandId;
    redisTemplate.opsForValue().set(key, action, SESSION_TTL);
  }

  public void bindStationSession(String stationId, String sessionId) {
    String key = "ocpp:station-session:" + stationId;
    redisTemplate.opsForValue().set(key, sessionId, SESSION_TTL);
  }

  public int reserveTransactionId(String stationId) {
    String key = "ocpp:transaction-seq:" + stationId;
    Long value = redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, SESSION_TTL);
    if (value == null || value <= 0) {
      return 1;
    }
    return Math.toIntExact(value);
  }

  public void storeTransactionConnector(String stationId, int transactionId, int connectorId) {
    String key = transactionConnectorKey(stationId, transactionId);
    redisTemplate.opsForValue().set(key, Integer.toString(connectorId), SESSION_TTL);
  }

  public OptionalInt findTransactionConnector(String stationId, int transactionId) {
    String key = transactionConnectorKey(stationId, transactionId);
    String value = redisTemplate.opsForValue().get(key);
    if (value == null || value.isBlank()) {
      return OptionalInt.empty();
    }
    try {
      return OptionalInt.of(Integer.parseInt(value));
    } catch (NumberFormatException ignored) {
      return OptionalInt.empty();
    }
  }

  private String transactionConnectorKey(String stationId, int transactionId) {
    return "ocpp:transaction-connector:" + stationId + ":" + transactionId;
  }
}
