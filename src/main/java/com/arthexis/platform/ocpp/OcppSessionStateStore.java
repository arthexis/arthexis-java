package com.arthexis.platform.ocpp;

import java.time.Duration;
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
}
