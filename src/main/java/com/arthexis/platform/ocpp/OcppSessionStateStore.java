package com.arthexis.platform.ocpp;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcppSessionStateStore {

  private static final Duration SESSION_TTL = Duration.ofHours(12);
  private static final String ACTIVE_SESSION_PREFIX = "ocpp:session:";
  private static final String PENDING_COMMAND_PREFIX = "ocpp:pending:";
  private final StringRedisTemplate redisTemplate;

  public OcppSessionStateStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void storePendingCommand(String stationId, String commandId, String action) {
    String key = PENDING_COMMAND_PREFIX + stationId + ":" + commandId;
    redisTemplate.opsForValue().set(key, action, SESSION_TTL);
  }

  public void storeActiveSession(String stationId, String sessionId) {
    redisTemplate.opsForValue().set(ACTIVE_SESSION_PREFIX + stationId, sessionId, SESSION_TTL);
  }

  public void removeActiveSession(String stationId) {
    redisTemplate.delete(ACTIVE_SESSION_PREFIX + stationId);
  }
}
